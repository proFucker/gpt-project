appProcessId=`ps -ef | grep java | grep "gpt-app" | awk 'BEGIN{FS=" "}{print $2}'`

if  [ x"$appProcessId" != "" ];then
  kill $$appProcessId
fi

projectHead=$1

if  [ x"$projectHead" != "" ];then
  git checkout $projectHead
  git pull
else
  git checkout master
  git pull
fi

./gradlew :gpt-app:clean
./gradlew :gpt-app:bootJar

gradleProcessId=`ps -ef | grep java | grep "gpt-app" | awk 'BEGIN{FS=" "}{print $2}'`
kill $gradleProcessId

nohup $JAVA_HOME/java -server -XX:-OmitStackTraceInFastThrow -XX:+UseG1GC  -Xms128m -Xmx128m -jar ./gpt-app/build/libs/gpt-app-1.0.jar --spring.profiles.active=prod > myout.file 2>&1 &