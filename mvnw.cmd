@echo off
REM SecGuard Maven Wrapper - 使用 Java 25 + Maven 3.9.9
REM 不影响系统全局的 Java 8 / Maven 3.6.3 配置

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
set "MAVEN_HOME=E:\apache-maven-3.9.9"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

mvn %*
