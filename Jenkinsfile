// =============================================================================
// Pipeline Jenkins Básico - US-04
// Ejecuta: mvn clean verify (checkout + build + test)
// =============================================================================

pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                bat 'mvnw.cmd clean verify -B'
            }
        }

        // US-34: SonarQube - Si SonarQube está configurado en Jenkins (System -> SonarQube servers)
        stage('SonarQube Analysis') {
            steps {
                script {
                    try {
                        withSonarQubeEnv('SonarQube') {
                            bat 'mvnw.cmd sonar:sonar -B'
                        }
                    } catch (Exception e) {
                        echo "SonarQube no configurado - saltando análisis: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            // US-35: Publicar Allure Reports
            allure includeProperties: false,
                   jdk: '',
                   results: [[path: '**/allure-results']]
        }
        failure {
            echo 'Pipeline falló - revisar logs'
        }
        success {
            echo 'Pipeline exitoso'
        }
    }
}
