#!groovy

node {
    stage('Checkout') {
        checkout scm
    }

    stage('Build') {
        gradle "clean build"

        stash name: "sources"
    }
}

parallel (
	"lint" : {
		node {
			stage ('Lint') {
				deleteDir()
				unstash "sources"
				gradle "lint"
				androidLint()
			}
		}
	},
	"test1" : {
		node {
			stage('Unit Test') {
				deleteDir()
				unstash "sources"

				gradle "test"

				junit '**/build/test-results/*/*.xml'
			}
		}
	},
	"test2" : {
		node {
			stage('Connected Test') {
				deleteDir()
				unstash "sources"

				gradle "cAT"

				junit '**/build/outputs/androidTest-results/connected/*.xml'
			}
		}
	}
)

node {
	stage ('Package') {
		deleteDir()
		unstash "sources"
		gradle "assemble"
		archiveArtifacts artifacts: '**/build/outputs/apk/*.apk', fingerprint: true, onlyIfSuccessful: true
	}
}

//Run gradle
void gradle(def args) {
    if (isUnix()) {
        sh "./gradlew ${args}"
    } else {
        bat ".\\gradlew ${args}"
    }
}
