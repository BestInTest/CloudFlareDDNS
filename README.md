# CloudFlareDDNS

>A DDNS client to update your CloudFlare DNS type A records with your local WAN IP.

This was designed for people who are hosting a web server from home, but do not have a static IP address.
Program currently updates configured type A record with local IP every 60 minutes.

## How to add to autostart (Windows)
- Copy downloaded jar to some folder
- Open program by double-clicking to create config file
- Close program and open ddns-config.yml in text editor to configure
- Remember to save file and set "start-in-background" to true if you want to keep program silently running in background
- Right-click on .jar file and create shortcut
- Use Win+R and type "shell:startup" OR open manually ```C:\Users\USERNAME\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup```
- Put created shortcut in this folder
- Restart the computer and see if the program starts automatically

Video tutorial: https://youtu.be/ewOT4Y_4fzE

## Requirements
- Java 8 or newer (not headless!)

## Known bugs
- The auto task timer resets when the "Cancel" button is used in the "Auto refresh" settings GUI
- GUI can hide to the tray if the "Auto refresh" setting is changed and "start-in-background" or "--to-tray" is used