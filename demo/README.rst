To generate Eclipse or IntelliJ project:
========================================

::

  sbt/sbt eclipse
  sbt/sbt gen-idea

Run single application
======================

Run MongoDB

::

  mongod

Run application

::

  sbt/sbt run

Now you have a project running at
http://localhost:8000/ and https://localhost:4430/

These route will be availabele

::

	[INFO] Normal routes:
	GET     /                  matsuri.demo.action.LoginIndex
	GET     /admin             matsuri.demo.action.AdminIndex
	GET     /admin/msg         matsuri.demo.action.AdminLastMessage (action cache: 1 [min])
	GET     /admin/msg/:name   matsuri.demo.action.AdminUserMessages
	POST    /admin/user        matsuri.demo.action.AdminUserCreate
	GET     /admin/user/:name  matsuri.demo.action.AdminUserShow
	PUT     /admin/user/:name  matsuri.demo.action.AdminUserUpdate
	DELETE  /admin/user/:name  matsuri.demo.action.AdminUserDelete
	GET     /chat              matsuri.demo.action.ChatIndex
	GET     /login             matsuri.demo.action.LoginIndex
	POST    /login             matsuri.demo.action.Login
	GET     /logout            matsuri.demo.action.Logout
	[INFO] SockJS routes:
	/connect  matsuri.demo.action.ChatAction  websocket: true, cookie_needed: false
	[INFO] Error routes:
	404  matsuri.demo.action.NotFoundError
	500  matsuri.demo.action.ServerError


Run clustered applications
==========================

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