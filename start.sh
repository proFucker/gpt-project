appProcessId=`ps -ef | grep java | grep "gpt-app" | awk 'BEGIN{FS=" "}{print $2}'`

echo $appProcessId

if  [ -n "$appProcessId" ];then
  kill $appProcessId
fi

projectHead=$1

echo $projectHead
echo $1

if  [ -n "$projectHead" ];then
  echo "tag"
  git checkout $projectHead
  git pull
else
  echo "master"
  git checkout master
  git pull
fi

#./gradlew :gpt-app:clean
rm ./gpt-app/build/libs/gpt-app-1.0.jar
./gradlew :gpt-app:bootJar

gradleProcessId=`ps -ef | grep java | grep -i "gradle" | awk 'BEGIN{FS=" "}{print $2}'`
kill $gradleProcessId

nohup $JAVA_HOME/bin/java -server -XX:-OmitStackTraceInFastThrow -XX:+UseG1GC  -Xms128m -Xmx128m -jar ./gpt-app/build/libs/gpt-app-1.0.jar --spring.profiles.active=prod > myout.file 2>&1 &