FROM openjdk:latest

ADD target/scala-2.13/iceo-case-study-assembly-1.0.0.jar ./
ADD $FILE_PATH ./

CMD java -cp iceo-case-study-assembly-1.0.0.jar $MAIN_CLASS