# gpt-project
openai文档:https://platform.openai.com/docs/api-reference

部署遇到的问题:如何用gradle编译运行spring-boot

配置插件问题
gradle版本问题(和spring-boot插件版本匹配)
用插件自带的task(在idea的gradle task里面可以看到的都是可以执行的命令)
最终命令:./gradlew :gpt-app:bootJar
待解决:自定义jar名称，指定jar的位置等

运行命令
nohup ~/java8/jdk1.8.0_202/bin/java -server -XX:-OmitStackTraceInFastThrow -XX:+UseG1GC  -jar gpt-app-1.0.jar > myout.file 2>&1 & 
#-Xss512K



