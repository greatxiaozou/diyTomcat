del /q bootstrap.jar
jar cvf0 bootstrap.jar -C target/classes top/greatxiaozou/diyTomcat/Bootstrap.class -C target/classes top/greatxiaozou/diyTomcat/classLoader/CommonClassLoader.class
del /q lib/diyTomcat.jar
cd target
cd classes
jar cvf0 ../../lib/diyTomcat.jar *
cd ..
cd ..
java -cp bootstrap.jar top.greatxiaozou.diyTomcat.Bootstrap
pause