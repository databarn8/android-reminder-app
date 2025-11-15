find . -type f -name "*.kt" | grep -i input

./gradlew assembleDebug
### find link or JAVA_HOME
readlink -f $(which java)
