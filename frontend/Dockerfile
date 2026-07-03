FROM node:20-alpine AS build

WORKDIR /app
COPY package*.json ./

RUN npm install

COPY . .
RUN npm run build

FROM tomcat:9.0-jdk11

RUN rm -rf /usr/local/tomcat/webapps/*

#For Vite React 

COPY --from=build /app/dist /usr/local/tomcat/webapps/ROOT

EXPOSE 8080

CMD ["catalina.sh","run"]