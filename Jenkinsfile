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
        SONAR_ORGANIZATION = 'yssnogood'
        JDK17_WINDOWS_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.18.8-hotspot'
        MAVEN_OPTS = '-Xmx2048m'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = isUnix()
                        ? sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        : bat(script: '@git rev-parse --short HEAD', returnStdout: true).trim()
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                }
            }
        }
        
        stage('Build & Unit Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw clean package -DskipTests=false -Dmaven.test.failure.ignore=false'
                    } else {
                        bat '''
                            set "JAVA_HOME=%JDK17_WINDOWS_HOME%"
                            set "PATH=%JAVA_HOME%\\bin;%PATH%"
                            call mvnw.cmd -v
                            call mvnw.cmd clean package -DskipTests=false -Dmaven.test.failure.ignore=false
                        '''
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                    jacoco path: '**/target/jacoco.exec'
                }
            }
        }
         
        stage('SonarCloud Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                    withSonarQubeEnv("${SONARQUBE_SERVER}") {
                        script {
                            if (isUnix()) {
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
                            } else {
                                bat '''
                                    set "JAVA_HOME=%JDK17_WINDOWS_HOME%"
                                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                                    call mvnw.cmd -v
                                    call mvnw.cmd sonar:sonar ^
                                        -Dsonar.host.url=https://sonarcloud.io ^
                                        -Dsonar.token=%SONAR_TOKEN% ^
                                        -Dsonar.organization=%SONAR_ORGANIZATION% ^
                                        -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                                        -Dsonar.projectName="Travel Plan" ^
                                        -Dsonar.java.coveragePlugin=jacoco ^
                                        -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml ^
                                        -Dsonar.qualitygate.wait=true
                                '''
                            }
                        }
                    }
                }
            }
        }
        
        // Quality Gate is handled by -Dsonar.qualitygate.wait=true in the SonarCloud Analysis stage
        
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
                        if (isUnix()) {
                            sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${DOCKER_REGISTRY}/${service}:${BUILD_VERSION}"
                        } else {
                            bat "trivy image --severity HIGH,CRITICAL --exit-code 1 ${DOCKER_REGISTRY}/${service}:${BUILD_VERSION}"
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                allOf {
                    branch 'develop'
                    expression { isUnix() }
                }
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
                script {
                    if (isUnix()) {
                        sh './mvnw verify -Pintegration-tests -Dtest.environment=staging'
                    } else {
                        bat '''
                            set "JAVA_HOME=%JDK17_WINDOWS_HOME%"
                            set "PATH=%JAVA_HOME%\\bin;%PATH%"
                            call mvnw.cmd -v
                            call mvnw.cmd verify -Pintegration-tests -Dtest.environment=staging
                        '''
                    }
                }
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                allOf {
                    branch 'main'
                    expression { isUnix() }
                }
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
                allOf {
                    anyOf {
                        branch 'main'
                        branch 'develop'
                    }
                    expression { isUnix() }
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
        success {
            script {
                def shortCommit = env.GIT_COMMIT_SHORT ?: 'n/a'
                def msg = "Build #${BUILD_NUMBER} succeeded for ${JOB_NAME} (${shortCommit})"
                try {
                    slackSend(color: 'good', message: msg)
                } catch (Exception ignored) {
                    echo msg
                }
            }
        }
        failure {
            script {
                def shortCommit = env.GIT_COMMIT_SHORT ?: 'n/a'
                def msg = "Build #${BUILD_NUMBER} failed for ${JOB_NAME} (${shortCommit})"
                try {
                    slackSend(color: 'danger', message: msg)
                } catch (Exception ignored) {
                    echo msg
                }
            }
        }
    }
}
