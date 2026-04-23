pipeline {
    agent any

    parameters {
        booleanParam(name: 'RUN_ANSIBLE_EVIDENCE', defaultValue: false, description: 'Run Ansible evidence workflow (double-run idempotence)')
        string(name: 'ANSIBLE_ENVIRONMENT', defaultValue: 'staging', description: 'Ansible environment variable value')
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 90, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }
    
    environment {
        DOCKER_IMAGE_PREFIX = 'travel-plan'
        SONARQUBE_SERVER = 'SonarCloud'
        SONAR_PROJECT_KEY = 'Yssnogood_travel-plan'
        SONAR_ORGANIZATION = 'yssnogood'
        JDK17_WINDOWS_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.18.8-hotspot'
        MAVEN_OPTS = '-Xmx2048m'
        COMPOSE_PROJECT = 'travel-plan'
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
                                        -Dsonar.qualitygate.wait=false
                                '''
                            } else {
                                bat '''
                                    set "JAVA_HOME=%JDK17_WINDOWS_HOME%"
                                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                                    call mvnw.cmd sonar:sonar ^
                                        -Dsonar.host.url=https://sonarcloud.io ^
                                        -Dsonar.token=%SONAR_TOKEN% ^
                                        -Dsonar.organization=%SONAR_ORGANIZATION% ^
                                        -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                                        -Dsonar.projectName="Travel Plan" ^
                                        -Dsonar.java.coveragePlugin=jacoco ^
                                        -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml ^
                                        -Dsonar.qualitygate.wait=false
                                '''
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                script {
                    def services = ['auth-service', 'user-service', 'travel-service', 'payment-service', 'notification-service']
                    services.each { service ->
                        echo "Building Docker image for ${service}..."
                        if (isUnix()) {
                            sh "docker build -t ${DOCKER_IMAGE_PREFIX}/${service}:${BUILD_VERSION} -t ${DOCKER_IMAGE_PREFIX}/${service}:latest -f services/${service}/Dockerfile ."
                        } else {
                            bat "docker build -t ${DOCKER_IMAGE_PREFIX}/${service}:${BUILD_VERSION} -t ${DOCKER_IMAGE_PREFIX}/${service}:latest -f services/${service}/Dockerfile ."
                        }
                    }
                    // Build admin dashboard
                    echo "Building Docker image for admin-dashboard..."
                    if (isUnix()) {
                        sh "docker build -t ${DOCKER_IMAGE_PREFIX}/admin-dashboard:${BUILD_VERSION} -t ${DOCKER_IMAGE_PREFIX}/admin-dashboard:latest -f admin-dashboard/Dockerfile admin-dashboard/"
                    } else {
                        bat "docker build -t ${DOCKER_IMAGE_PREFIX}/admin-dashboard:${BUILD_VERSION} -t ${DOCKER_IMAGE_PREFIX}/admin-dashboard:latest -f admin-dashboard/Dockerfile admin-dashboard/"
                    }
                    echo "All Docker images built successfully"
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    def services = ['auth-service', 'user-service', 'travel-service', 'payment-service', 'notification-service']
                    def scanResults = [:]
                    
                    services.each { service ->
                        echo "Scanning ${service} for vulnerabilities..."
                        try {
                            if (isUnix()) {
                                sh "trivy image --severity HIGH,CRITICAL --exit-code 0 --format table ${DOCKER_IMAGE_PREFIX}/${service}:${BUILD_VERSION}"
                            } else {
                                bat "trivy image --severity HIGH,CRITICAL --exit-code 0 --format table ${DOCKER_IMAGE_PREFIX}/${service}:${BUILD_VERSION}"
                            }
                            scanResults[service] = 'scanned'
                        } catch (Exception e) {
                            echo "Trivy scan skipped for ${service} (trivy not installed). Install with: choco install trivy"
                            scanResults[service] = 'skipped'
                        }
                    }
                    
                    echo "Security scan summary: ${scanResults}"
                }
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                script {
                    echo "Deploying to staging environment via Docker Compose..."
                    if (isUnix()) {
                        sh '''
                            docker compose -p ${COMPOSE_PROJECT} -f docker/docker-compose.infra.yml -f docker/docker-compose.services.yml down --remove-orphans
                            for c in travel-postgres-primary travel-postgres-replica travel-neo4j travel-redis travel-rabbitmq travel-vault travel-api-gateway travel-auth-service travel-user-service travel-travel-service travel-payment-service travel-notification-service travel-admin-dashboard; do docker rm -f $c 2>/dev/null || true; done
                            docker compose -p ${COMPOSE_PROJECT} -f docker/docker-compose.infra.yml up -d
                            echo "Waiting for infrastructure services to initialize..."
                            sleep 30
                            docker compose -p ${COMPOSE_PROJECT} -f docker/docker-compose.infra.yml -f docker/docker-compose.services.yml up -d --no-build
                            echo "Staging deployment complete"
                        '''
                    } else {
                        bat '''
                            docker compose -p %COMPOSE_PROJECT% -f docker/docker-compose.infra.yml -f docker/docker-compose.services.yml down --remove-orphans
                            for %%c in (travel-postgres-primary travel-postgres-replica travel-neo4j travel-redis travel-rabbitmq travel-vault travel-api-gateway travel-auth-service travel-user-service travel-travel-service travel-payment-service travel-notification-service travel-admin-dashboard) do docker rm -f %%c 2>nul
                            docker compose -p %COMPOSE_PROJECT% -f docker/docker-compose.infra.yml up -d
                            echo Waiting for infrastructure services to initialize...
                            ping -n 31 127.0.0.1 >nul
                            docker compose -p %COMPOSE_PROJECT% -f docker/docker-compose.infra.yml -f docker/docker-compose.services.yml up -d --no-build
                            echo Staging deployment complete
                        '''
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw verify -Pintegration-tests -Dtest.environment=staging'
                    } else {
                        bat '''
                            set "JAVA_HOME=%JDK17_WINDOWS_HOME%"
                            set "PATH=%JAVA_HOME%\\bin;%PATH%"
                            call mvnw.cmd verify -Pintegration-tests -Dtest.environment=staging
                        '''
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
                }
            }
        }
        
        stage('Deploy to Production') {
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
                
                script {
                    echo "Deploying to production environment..."
                    if (isUnix()) {
                        sh '''
                            docker compose -p ${COMPOSE_PROJECT} -f docker/docker-compose.infra.yml -f docker/docker-compose.services.yml up -d --no-build --force-recreate
                            echo "Production deployment complete"
                        '''
                    } else {
                        bat '''
                            docker compose -p %COMPOSE_PROJECT% -f docker/docker-compose.infra.yml -f docker/docker-compose.services.yml up -d --no-build --force-recreate
                            echo Production deployment complete
                        '''
                    }
                }
            }
        }
        
        stage('Smoke Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh './scripts/smoke-tests.sh local'
                    } else {
                        bat 'powershell -ExecutionPolicy Bypass -File scripts\\smoke-tests.ps1'
                    }
                }
            }
        }

        stage('Ansible Evidence') {
            when {
                expression { return params.RUN_ANSIBLE_EVIDENCE }
            }
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            docker run --rm \
                              -v "$PWD:/workspace" \
                              -w /workspace \
                              -e INVENTORY=ansible/inventory/hosts.yml \
                              -e PLAYBOOK=ansible/playbooks/deploy-all.yml \
                              -e ENVIRONMENT=${ANSIBLE_ENVIRONMENT} \
                              cytopia/ansible:latest \
                              bash -lc "chmod +x scripts/run-ansible-evidence.sh; ./scripts/run-ansible-evidence.sh"
                        '''
                    } else {
                        bat '''
                            docker run --rm ^
                              -v "%CD%:/workspace" ^
                              -w /workspace ^
                              -e INVENTORY=ansible/inventory/hosts.yml ^
                              -e PLAYBOOK=ansible/playbooks/deploy-all.yml ^
                              -e ENVIRONMENT=%ANSIBLE_ENVIRONMENT% ^
                              cytopia/ansible:latest ^
                              bash -lc "chmod +x scripts/run-ansible-evidence.sh; ./scripts/run-ansible-evidence.sh"
                        '''
                    }
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'artifacts/ansible/**/*', allowEmptyArchive: true
        }
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
