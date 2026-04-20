pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }
    
    environment {
        DOCKER_REGISTRY = 'registry.travelplan.com'
        DOCKER_CREDENTIALS_ID = 'docker-registry-creds'
        SONARQUBE_SERVER = 'SonarCloud'
        SONAR_PROJECT_KEY = 'Yssnogood_travel-plan'
        SONAR_ORGANIZATION = 'travel-plan-org'
        MAVEN_OPTS = '-Xmx2048m'
        JAVA_HOME = tool 'JDK17'
        PATH = "${JAVA_HOME}/bin:${PATH}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                }
            }
        }
        
        stage('Build & Unit Tests') {
            steps {
                sh '''
                    ./mvnw clean package -DskipTests=false \
                        -Dmaven.test.failure.ignore=false
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: '**/target/*.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/test/**'
                    )
                }
            }
        }
        
        stage('SonarCloud Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                    withSonarQubeEnv("${SONARQUBE_SERVER}") {
                        sh '''
                            ./mvnw sonar:sonar \
                                -Dsonar.host.url=https://sonarcloud.io \
                                -Dsonar.token=${SONAR_TOKEN} \
                                -Dsonar.organization=${SONAR_ORGANIZATION} \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.projectName="Travel Plan" \
                                -Dsonar.java.coveragePlugin=jacoco \
                                -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml \
                                -Dsonar.qualitygate.wait=true
                        '''
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Build Docker Images') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    branch pattern: "release/*", comparator: "GLOB"
                }
            }
            steps {
                script {
                    def services = ['auth-service', 'user-service', 'travel-service', 'payment-service', 'notification-service']
                    
                    docker.withRegistry("https://${DOCKER_REGISTRY}", DOCKER_CREDENTIALS_ID) {
                        services.each { service ->
                            def image = docker.build("${DOCKER_REGISTRY}/${service}:${BUILD_VERSION}", 
                                "-f services/${service}/Dockerfile .")
                            image.push()
                            image.push('latest')
                        }
                    }
                }
            }
        }
        
        stage('Security Scan') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    def services = ['auth-service', 'user-service', 'travel-service', 'payment-service', 'notification-service']
                    
                    services.each { service ->
                        sh """
                            trivy image --severity HIGH,CRITICAL \
                                --exit-code 1 \
                                ${DOCKER_REGISTRY}/${service}:${BUILD_VERSION}
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    sshagent(['ansible-deploy-key']) {
                        sh '''
                            cd ansible
                            ansible-playbook -i inventory/hosts.yml \
                                playbooks/deploy-all.yml \
                                --limit staging \
                                -e "service_version=${BUILD_VERSION}"
                        '''
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                branch 'develop'
            }
            steps {
                sh '''
                    ./mvnw verify -Pintegration-tests \
                        -Dtest.environment=staging
                '''
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
                
                script {
                    sshagent(['ansible-deploy-key']) {
                        sh '''
                            cd ansible
                            ansible-playbook -i inventory/hosts.yml \
                                playbooks/deploy-all.yml \
                                --limit production \
                                -e "service_version=${BUILD_VERSION}"
                        '''
                    }
                }
            }
        }
        
        stage('Smoke Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    def environment = env.BRANCH_NAME == 'main' ? 'production' : 'staging'
                    sh """
                        ./scripts/smoke-tests.sh ${environment}
                    """
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend(
                color: 'good',
                message: "Build #${BUILD_NUMBER} succeeded for ${JOB_NAME} (${GIT_COMMIT_SHORT})"
            )
        }
        failure {
            slackSend(
                color: 'danger',
                message: "Build #${BUILD_NUMBER} failed for ${JOB_NAME} (${GIT_COMMIT_SHORT})"
            )
        }
    }
}
