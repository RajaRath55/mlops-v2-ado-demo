pipeline {
agent { docker { image 'python:3.8.16' } }
    stage('init') {
      checkout scm
    }

    stage('InstallDependencies'){
        sh '''
        python -m pip install --upgrade pip
      	pip install pytest codecov pydocstyle pytest-cov pylint pylint_junit flake8==3.7.* flake8_formatter_junit_xml==0.0.*
      	pip install -r data-science/environment/train-requirements.txt
        '''
    }

    
    stage('RunUnitTests'){
        sh '''
      	flake8 data-science/src/ --output-file=flake8-testresults.xml --format junit-xml --exit-zero
      	pylint data-science/src/ --output-format=pylint_junit.JUnitReporter --exit-zero > pylint-testresults.xml
      	pytest -v data-science/src/*/test_*.py --doctest-modules --junitxml=unit-testresults.xml --cov=src --cov-append --cov-report=xml:coverage.xml --cov-report=html:htmlcov
        '''
    }
}
