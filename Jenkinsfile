// =============================================================================
// Pipeline Jenkins - US-04 / US-34 / US-35
// Stages: Unit Tests → Integration Tests → SonarQube Analysis
// =============================================================================

pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
    }

    stages {
        // US-04: Tests unitarios (sin API key, sin DB real — usa H2 en memoria)
        stage('Unit Tests') {
            steps {
                sh 'mvn clean test'
            }
        }

        // Tests de integración: requiere TWELVE_DATA_API_KEY en Jenkins Credentials
        // Credential: Kind=Secret text, ID=twelve-data-api-key
        // Omite re-ejecutar unit tests (-Dsurefire.skip=true) y activa perfil -Pintegration
        stage('Integration Tests') {
            steps {
                withCredentials([string(credentialsId: 'TWELVE_DATA_API_KEY', variable: 'TWELVE_DATA_API_KEY')]) {
                    sh 'mvn verify -Pintegration -Dsurefire.skip=true'
                }
            }
        }

        // US-34: SonarQube — analiza sobre los reportes JaCoCo ya generados en stages anteriores
        stage('SonarQube Analysis') {
            steps {
                script {
                    try {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                            mvn sonar:sonar \
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
