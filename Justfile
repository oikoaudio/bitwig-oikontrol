set shell := ["bash", "-cu"]

gradle_user_home := env_var_or_default("GRADLE_USER_HOME", "/tmp/gradle-home")
gradlew := "./gradlew --no-daemon"
bitwig_extensions_dir := env_var_or_default("BITWIG_EXTENSIONS_DIR", env_var("HOME") + "/Documents/Bitwig Studio/Extensions")

default:
    @just --list

build:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} build

test:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} test

clean:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} clean

fire-compile:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:akai-fire:compileJava :modules:akai-fire:testClasses

fire-test:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:akai-fire:test

fire-build:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:akai-fire:build

fire-extension:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:akai-fire:jar
    @find modules/akai-fire/build/libs -maxdepth 1 -type f -name '*.bwextension' | sort

fire-install:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:akai-fire:jar
    mkdir -p "{{bitwig_extensions_dir}}"
    cp "$(find modules/akai-fire/build/libs -maxdepth 1 -type f -name 'AkaiFireOiko-*.bwextension' -printf '%T@ %p\n' | sort -n | tail -n 1 | cut -d' ' -f2-)" "{{bitwig_extensions_dir}}/"
    @find "{{bitwig_extensions_dir}}" -maxdepth 1 -type f -name 'AkaiFireOiko-*.bwextension' -printf '%T@ %p\n' | sort -n | tail -n 1 | cut -d' ' -f2-

launchcontrol-build:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:launchcontrol:build

launchcontrol-test:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:launchcontrol:test

launchcontrol-extension:
    GRADLE_USER_HOME={{gradle_user_home}} {{gradlew}} :modules:launchcontrol:jar
    @find modules/launchcontrol/build/libs -maxdepth 1 -type f -name '*.bwextension' | sort

artifacts:
    @find modules -type f \( -name '*.bwextension' -o -name '*.jar' \) | sort
