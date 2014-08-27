To generate Eclipse or IntelliJ project:
========================================

::

  sbt/sbt eclipse
  sbt/sbt gen-idea

Run
===

Run MongoDB

::

  mongod

::

  sbt/sbt run

Now you have a new empty skeleton project running at
http://localhost:8000/ and https://localhost:4430/




Cluster
=======

Package project

::

	sbt/sbt xitrum-package

Copy packaged application 3 times

::

	cp -r target/xitrum /path/to/temp/directory/node1
	cp -r target/xitrum /path/to/temp/directory/node2
	cp -r target/xitrum /path/to/temp/directory/node3

Override `config` directory from `cluster_config`

::

	cp cluster_config/node1/* /path/to/temp/directory/node1/config/
	cp cluster_config/node2/* /path/to/temp/directory/node2/config/
	cp cluster_config/node3/* /path/to/temp/directory/node3/config/

Run 2 application from separate terminal
node1 will listen http://localhost:8001, node2 will listen http://localhost:8002 and node3 will listen http://localhost:8003.

::

	cd /path/to/temp/directory/node1
	./script/runner matsuri.demo.Boot

::

	cd /path/to/temp/directory/node2
	./script/runner matsuri.demo.Boot

::

	cd /path/to/temp/directory/node3
	./script/runner matsuri.demo.Boot

Run HAProxy

::

	 haproxy -f config/haproxy.cfg

instances will be balanced behind  http://localhost:9000