# SuperApp

SuperApp - графический файловый менеджер на Java.

## Как теперь работать с проектом

Maven сам умеет:

- собирать проект
- запускать программу
- собирать исполняемый jar

Исходники остаются в `System/src/main/java`, Maven берет их оттуда через настройку `sourceDirectory` в `pom.xml`.

## Требования

- Java 21
- Maven 3.9+

## Команды

Собрать jar:

```bash
mvn clean package
```

После этого jar будет лежать в:

```bash
target/SuperApp.jar
```

Собрать и сразу запустить программу через Maven:

```bash
mvn clean package exec:java
```

Быстрый запуск без упаковки jar:

```bash
mvn compile exec:java
```

## Что настроено в pom.xml

- `maven-compiler-plugin` компилирует проект под Java 21
- `exec-maven-plugin` запускает главный класс `filesystem.MainWindow`
- `maven-shade-plugin` собирает готовый `target/SuperApp.jar` со всеми зависимостями

Отдельный shell-скрипт для запуска больше не нужен: Maven умеет и собирать, и запускать проект сам.
