pipeline {
    agent any

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -q'
            }
        }

        stage('Test & Coverage') {
            steps {
                sh 'mvn verify'
                junit 'target/surefire-reports/*.xml'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                sh 'mvn sonar:sonar -Dsonar.host.url=http://sonarqube:9000'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully! All modules passed.'
        }
        failure {
            echo "Pipeline failed at stage: ${env.STAGE_NAME}"
            echo 'Check Console Output and look for [ERROR] or FAILURE in the Reactor Summary.'
        }
        unstable {
            echo 'Pipeline is unstable — test failures detected. Check the Test Results tab.'
        }
    }
}