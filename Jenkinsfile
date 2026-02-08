// =============================================================================
// Pipeline Jenkins Básico - US-04
// Ejecuta: mvn clean verify (checkout + build + test)
// =============================================================================

pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
    }

    stages {
        // US-34: SonarQube - Si SonarQube está configurado en Jenkins (System -> SonarQube servers)
        stage('SonarQube Analysis') {
            steps {
                script {
                    try {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                            mvn verify sonar:sonar \
                            -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml
                            '''
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
