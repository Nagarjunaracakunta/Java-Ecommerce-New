pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                dir('/var/jenkins_home/project') {
                    sh 'mvn clean compile -q'
                }
            }
        }

        stage('Test & Coverage') {
            steps {
                dir('/var/jenkins_home/project') {
                    sh 'mvn verify'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('/var/jenkins_home/project') {
                    sh 'mvn sonar:sonar -Dsonar.host.url=http://sonarqube:9000'
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}