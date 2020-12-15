# SinLing Toolkit as a Word Splitter

This is the [SinLing project](https://github.com/ysenarath/sinling) as a Word Splitter for Sinhala language

## Instructions for server deployment (Inside CentOS 7 VM)

``` NOTE: VM should have installed nginx server and python 3 before proceed with below steps```

``IMPORTANT!! Refer SinLing project installation guid before start to setup``

1. Create a python virtual environment `python3 -m venv splitterEnv`
2. Install nltk(3.5), flask(1.1.2), gunicorn(20.0.4) and emoji(0.6.0) within the envirenment `pip install emoji nltk gunicorn flask`
3. Add project files into the VM
4. Create a system service using `sudo vim /etc/systemd/system/flair.service`
5. Edit the file content as follows

    ```
    [Unit]
    Description=Service to serve sinhala word splitter
    After=network.target
    
    [Service]
    User=<<Username>>
    Group=nginx
    WorkingDirectory=<<Project absolute path>> [remove this!! eg: /home/<<username>>/sinhalaWordsplitter]
    Environment="PATH=<<Python envirenment bin folder path>>"  [remove this!! eg: /home/<<username>>/splitterEnv/bin]
    ExecStart=<<Python envirenment bin folder path>>/gunicorn --workers 1 --bind unix:splitter.sock -m 007 wsgi
    
    [Install]
    WantedBy=multi-user.target
    ```

6. Start and enable the created service

    ```
   sudo systemctl start splitter
   sudo systemctl enable splitter
    ```

7. Let's configure nginx server
8. Open nginx default configuration file `sudo vim /etc/nginx/nginx.conf`
9. insert following configurations under the default server block. NOTE : Here we used default http server to access the service. Otherwise if you use another server create new server block in the conf file. For more info refer attached tutorial

    ```
    location /split{
       proxy_pass http://unix:<<project absolute path>>/splitter.sock;
    }
    ```

10. Start and enable nginx service

    ```
    sudo systemctl start nginx
    sudo systemctl enable nginx
    ```
    
11. run below commands to configure the nginx (only one time after installing nginx)
    ```
    sudo yum install policycoreutils-devel
    sudo setsebool httpd_can_network_connect on -P
    sudo usermod -a -G <<username>> nginx
    chmod 710 /home/<<username>> (if not work use sudo)
    sudo systemctl restart nginx
    sudo systemctl restart splitter
    sudo cat /var/log/audit/audit.log | grep nginx | grep denied | audit2allow -M mynginx
    sudo semodule -i mynginx.pp
    sudo systemctl restart nginx
    sudo systemctl restart splitter
    ```

11. Access the service sending post request to `http://serveraddress/getpredicates`
`NOTE: Object structure {"word":""}`

[TUTORIAL](https://www.digitalocean.com/community/tutorials/how-to-serve-flask-applications-with-gunicorn-and-nginx-on-centos-7)

DONE!!