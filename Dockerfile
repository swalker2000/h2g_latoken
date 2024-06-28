FROM amazoncorretto:17.0.8-alpine
EXPOSE $PORT
RUN mkdir /opt/app
RUN mkdir /opt/app/images
COPY build/libs/latoken*.jar /opt/app/app.jar
WORKDIR /opt/app
CMD ["java", "-jar", "app.jar"]