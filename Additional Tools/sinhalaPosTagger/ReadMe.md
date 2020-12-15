# Sinhala tnt PosTagger

This is the Sinhala PosTagger using tnt tool

## Instructions for server deployment (Inside CentOS 7 VM)

``` NOTE: VM should have installed nginx server, maven and jdk 1.8 before proceed with below steps```
1. Add the project into the VM
2. Build the project using maven `mvn clean package`
3. Move jar file into project root directory (jar should be in same directory with tnt folder)
4. install glibc using `sudo yum install glibc.i686`
5. Create a script to update Path variable of the instance using `sudo vim /etc/profile.d/script.sh`
6. Edit the script as follows

    ```
    export PATH=/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/home/<<username>>/.local/bin:/home/<<username>>/bin:/home/<<username>>/sinhalaPosTagger/tnt
    ```
    ``Note: above script for add tnt folder path into PATH variables. edit the above script according to the requirements of the instance``

7. Give execution permission to tnt/tnt file

    ```
   sudo chmod +x /home/<<username>>/sinhalaPosTagger/tnt/tnt
    ```

8. Let's configure nginx server
9. Open nginx default configuration file `sudo vim /etc/nginx/nginx.conf`
10. insert following configurations under the default server block. NOTE : Here we used default http server to access the service. Otherwise if you use another server create new server block in the conf file. For more info refer attached tutorial

    ```
    location /getpos{
       proxy_pass http://localhost:3002/getpos;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
       proxy_set_header X-Forwarded-Port $server_port;
    }
    ```

10. Start and enable nginx service

    ```
    sudo systemctl start nginx
    sudo systemctl enable nginx
    ```
    if already started restart it `sudo systemctl restart nginx`
    
11. create a start script to start the service as background process
    ```
        java -jar /home/<<username>>/sinhalaPosTagger/pos-0.0.1-SNAPSHOT.jar --server.port=3002 > /home/<<username>>/
        sinhalaPosTagger/pos.log 2>/home/<<username>>/sinhalaPosTagger/pos.error.log &
    ```
11. Access the service sending post request `http://serveraddress/getpos`
`NOTE: Object structure {"sentence":""}`

12. Please make sure sure to change the file paths in ProcessSentence.java file into absolute paths if there is any error regarding accessing files

[TUTORIAL](https://www.digitalocean.com/community/tutorials/how-to-serve-flask-applications-with-gunicorn-and-nginx-on-centos-7)

DONE!!