# Flair Predicate Identifier

This is English predicate identifier using Flair toolkit

## Instructions for server deployment (Inside CentOS 7 VM)

``` NOTE: VM should have installed nginx server and python 3 before proceed with below steps```
1. Create a python virtual environment `python3 -m venv flairEnv`
2. Install flair(0.7), flask(1.1.2) and gunicorn(20.0.4) within the envirenment `pip install flair gunicorn flask`
3. Add project files into the VM
4. Create a system service using `sudo vim /etc/systemd/system/flair.service`
5. Edit the file content as follows

    ```
    [Unit]
    Description=English Predicate flair
    After=network.target
    
    [Service]
    User=<<Username>>
    Group=nginx
    WorkingDirectory=<<Project absolute path>> [remove this!! eg: /home/<<username>>/EnglishPredicateIdentifier]
    Environment="PATH=<<Python envirenment bin folder path>>"  [remove this!! eg: /home/<<username>>/flairEnv/bin]
    ExecStart=<<Python envirenment bin folder path>>/gunicorn --workers 1 --bind unix:flair.sock -m 007 wsgi
    
    [Install]
    WantedBy=multi-user.target
    ```

6. Start and enable the created service

    ```
   sudo systemctl start flair
   sudo systemctl enable flair
    ```

7. Let's configure nginx server
8. Open nginx default configuration file `sudo vim /etc/nginx/nginx.conf`
9. insert following configurations under the default server block. NOTE : Here we used default http server to access the service. Otherwise if you use another server create new server block in the conf file. For more info refer attached tutorial

    ```
    location /getpredicates{
       proxy_pass http://unix:<<project absolute path>>/flair.sock;
    }
    ```

10. Start and enable nginx service

    ```
    sudo systemctl start nginx
    sudo systemctl enable nginx
    ```
    
11. run below commands to configure the nginx(needed only one time after installing nginx)
    ```
    sudo yum install policycoreutils-devel
    sudo setsebool httpd_can_network_connect on -P
    sudo usermod -a -G <<username>> nginx
    sudo chmod 710 /home/<<username>>
    sudo systemctl restart nginx
    sudo systemctl restart flair
    sudo cat /var/log/audit/audit.log | grep nginx | grep denied | audit2allow -M mynginx
    sudo semodule -i mynginx.pp
    sudo systemctl restart nginx
    sudo systemctl restart flair
    ```

11. Access the service sending post request to `http://serveraddress/getpredicates`
`NOTE: Object structure {"word":""}`

[TUTORIAL](https://www.digitalocean.com/community/tutorials/how-to-serve-flask-applications-with-gunicorn-and-nginx-on-centos-7)

DONE!!
