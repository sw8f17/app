#!groovy

node {
    stage('Checkout') {
        checkout scm
    }

	stage ('Build toolchain') {
		sh "rm -r ./toolchain || exit 0"
		sh "./mktoolchains.sh /opt/Android/"
	}

	stage('Build Native') {
		sh "./mkdep.sh"
	}

    stage('Build') {
        gradle "clean build"

        stash name: "sources", excludes: "toolchain/"
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
	},
	"javadoc" : {
		node {
			stage('Javadoc') {
				deleteDir()
				unstash "sources"

				gradle "javadoc"
				publishHTML([
					allowMissing: false,
					alwaysLinkToLastBuild: false,
					keepAll: false,
					reportDir: 'javadoc',
					reportFiles: 'index.html',
					reportName: 'Javadoc'
				])
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
