# SuperApp

SuperApp - графический файловый менеджер для Linux на Java.

## Maven-сборка

- `pom.xml` находится в корне проекта

- итоговый исполняемый jar собирается в `target/SuperApp.jar`

В `pom.xml` Maven настроен на нестандартный путь к исходникам через `sourceDirectory`, поэтому переносить файлы не нужно.

## Требования

- Java 21
- Maven 3.9+

## Сборка

```bash
mvn clean package
```

После сборки jar будет лежать здесь:

```bash
target/SuperApp.jar
```

## Запуск

```bash
java -jar target/SuperApp.jar
```

или

```bash
./run-superapp.sh
```

