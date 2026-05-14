/*
 * ConnectSphere — Jenkins CI/CD Pipeline
 * =======================================
 *
 * This Jenkinsfile defines the CI/CD pipeline for all
 * ConnectSphere microservices.
 *
 * Pipeline stages:
 *   1. Checkout → pull code from Git
 *   2. Build → compile all services with Maven
 *   3. Test → run JUnit + Mockito tests
 *   4. SonarQube → code quality analysis
 *   5. Package → create JAR files
 *   6. Build Angular Frontend
 *
 * Prerequisites:
 *   - Jenkins with Maven plugin installed
 *   - SonarQube server configured
 *   - Maven 3.9+ and JDK 17 on Jenkins agent
 */

pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    environment {
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Pulling source code from Git...'
                checkout scm
            }
        }

        stage('Build All Services') {
            parallel {
                stage('Eureka Server') {
                    steps {
                        dir('eureka-server/eureka-server') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('API Gateway') {
                    steps {
                        dir('api-gateway/api-gateway') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Auth Service') {
                    steps {
                        dir('auth-service/auth-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Post Service') {
                    steps {
                        dir('post-service/post-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Payment Service') {
                    steps {
                        dir('payment-service/payment-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Admin Server') {
                    steps {
                        dir('admin-server/admin-server') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Comment Service') {
                    steps {
                        dir('comment-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Like Service') {
                    steps {
                        dir('like-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Follow Service') {
                    steps {
                        dir('follow-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Notification Service') {
                    steps {
                        dir('notification-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Media Service') {
                    steps {
                        dir('media-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
                stage('Search Service') {
                    steps {
                        dir('search-service') {
                            bat 'mvnw.cmd clean compile -q'
                        }
                    }
                }
            }
        }

        stage('Run Tests') {
            parallel {
                stage('Auth Tests') {
                    steps {
                        dir('auth-service/auth-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                    post {
                        always {
                            junit 'ConnectSphere-Backend/auth-service/auth-service/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Post Tests') {
                    steps {
                        dir('post-service/post-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
                stage('Payment Tests') {
                    steps {
                        dir('payment-service/payment-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
                stage('Comment Tests') {
                    steps {
                        dir('comment-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
                stage('Like Tests') {
                    steps {
                        dir('like-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
                stage('Follow Tests') {
                    steps {
                        dir('follow-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
                stage('Notification Tests') {
                    steps {
                        dir('notification-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
                stage('Media Tests') {
                    steps {
                        dir('media-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
                stage('Search Tests') {
                    steps {
                        dir('search-service') {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube code quality analysis...'
                dir('auth-service/auth-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-auth"
                }
                dir('post-service/post-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-post"
                }
                dir('payment-service/payment-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-payment"
                }
                dir('comment-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-comment"
                }
                dir('like-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-like"
                }
                dir('follow-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-follow"
                }
                dir('notification-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-notification"
                }
                dir('media-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-media"
                }
                dir('search-service') {
                    bat "mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.projectKey=connectsphere-search"
                }
            }
        }

        stage('Package JARs') {
            steps {
                echo '📦 Packaging all services as JAR files...'
                dir('eureka-server/eureka-server') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('api-gateway/api-gateway') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('auth-service/auth-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('post-service/post-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('payment-service/payment-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('admin-server/admin-server') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('comment-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('like-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('follow-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('notification-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('media-service') { bat 'mvnw.cmd package -DskipTests -q' }
                dir('search-service') { bat 'mvnw.cmd package -DskipTests -q' }
            }
        }

        stage('Build Angular Frontend') {
            steps {
                echo '🌐 Building Angular frontend...'
                dir('ConnectSphere-Frontend') {
                    bat 'npm install'
                    bat 'npx ng build --configuration=production'
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed. Check logs for errors.'
        }
        always {
            echo '📊 Pipeline finished.'
        }
    }
}
