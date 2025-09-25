# redLINK

An end-to-end telemetry system for our Shell Eco-marathon vehicle  
- **Mobile App (React Native)** → Captures data from vehicle via Bluetooth and GPS  
- **Server (Node.js + Socket.IO)** → Relays real-time data between devices  
- **Web Dashboard (React)** → Displays live vehicle data for monitoring and analysis

<img src="https://github.com/user-attachments/assets/8b8db57c-dbb5-43e1-873e-c2d54d18e5df" alt="Web App Screenshot" width="70%" />  

---

## 🚀 Getting Started
Clone the repository:
```bash
git clone https://github.com/ulassekerci/redLINK.git
```
### Server
Build and run the server
```bash
cd redLINK/server
npm install
npm run build
npm run start
```
### Web App
Navigate to the web folder and install dependencies
```bash
cd web
npm install
```
Create a .env file and enter your server url
```bash
VITE_SOCKET_URL="localhost:3000"
```
Build and run the web app
```bash
npm run build
npm run preview
```

### Mobile App
CD into mobile folder
```bash
cd redLINK/mobile
```
Create a .env file and enter your server url
```bash
EXPO_PUBLIC_SOCKET_URL="192.168.1.72:3000"
```
Refer to expo docs for building the mobile app for your preferred platform  
[https://docs.expo.dev/get-started/set-up-your-environment/](https://docs.expo.dev/get-started/set-up-your-environment/)
