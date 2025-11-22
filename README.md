# Matatu Pap Admin App

A comprehensive fleet management solution designed specifically for Matatu SACCOs and fleet managers in Kenya. This Android application provides powerful tools to monitor, analyze, and optimize your matatu operations with real-time performance insights.

## 🚌 Overview

Matatu Pap Admin App empowers SACCO managers and fleet owners to efficiently manage their matatu fleet by providing detailed performance analytics, route management, and comparative analysis tools. Make data-driven decisions to improve your fleet's profitability and operational efficiency.

## ✨ Key Features

### 🚍 Bus Management
- **Add and Register Buses**: Easily onboard new vehicles to your fleet with detailed information
- **Performance Monitoring**: Track individual bus performance metrics including:
  - Revenue generation
  - Fuel consumption
  - Trip completion rates
  - Maintenance schedules
  - Driver performance
- **Bus Comparison**: Compare performance between any two buses in your fleet to identify top performers and underperformers
- **Fleet Overview**: Get a comprehensive view of all buses and their real-time status

### 🗺️ Route Management
- **Google Maps Integration**: Add and manage routes using Google Maps API for accurate route planning
- **Route Performance Analytics**: Monitor route profitability and efficiency
- **Route Comparison**: Compare performance metrics between different routes
- **Route Optimization**: Identify the most profitable routes and optimize schedules

### 📊 Analytics & Reporting
- **Overall Performance Dashboard**: View aggregated performance metrics for your entire fleet
- **Real-time Insights**: Access up-to-date information on bus and route performance
- **Comparative Analysis**: Side-by-side comparison tools for buses and routes
- **Custom Reports**: Generate detailed reports for business decision-making

## 🛠️ Technology Stack

- **Platform**: Android (Java/Kotlin)
- **Maps Integration**: Google Maps API
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Firebase
- **Authentication**: Firebase

## 📋 Prerequisites

Before you begin, ensure you have:
- Android Studio (latest version recommended)
- Android SDK (API level 21 or higher)
- Google Maps API Key
- JDK 8 or higher

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/James-Muthama/Matatu-Pap-Admin-App.git
   cd Matatu-Pap-Admin-App
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository folder

3. **Configure Google Maps API**
   - Obtain a Google Maps API key from [Google Cloud Console](https://console.cloud.google.com/)
   - Add your API key to `local.properties`:
     ```
     MAPS_API_KEY=your_api_key_here
     ```
   - Or add it to `AndroidManifest.xml`:
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="YOUR_API_KEY" />
     ```

4. **Sync and Build**
   - Click "Sync Project with Gradle Files"
   - Build the project: `Build` → `Make Project`

5. **Run the App**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press `Shift + F10`

## 📱 Usage

### Getting Started
1. **Sign Up/Login**: Create an account or log in as a SACCO manager
2. **Add Your Fleet**: Register all your matatus with relevant details
3. **Set Up Routes**: Define your operational routes using the Google Maps interface
4. **Monitor Performance**: Access the dashboard to view real-time analytics

### Managing Buses
- Navigate to the "Buses" section
- Click "Add Bus" to register a new vehicle
- View individual bus details by tapping on any bus card
- Use the comparison tool to analyze performance between buses

### Managing Routes
- Go to the "Routes" section
- Click "Add Route" and use the map to define the route
- Monitor route performance metrics
- Compare routes to identify optimization opportunities

## 🔐 Security

- User authentication and authorization
- Secure data transmission
- Role-based access control for different user types
- Data encryption for sensitive information

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**James Muthama**
- GitHub: [@James-Muthama](https://github.com/James-Muthama)

## 📞 Support

For support, email support@matatupap.com or open an issue in the GitHub repository.

## 🙏 Acknowledgments

- Google Maps Platform for mapping services
- The Android developer community
- All contributors and testers

## 📈 Roadmap

- [ ] Real-time GPS tracking integration
- [ ] Mobile money payment integration
- [ ] Driver mobile app companion
- [ ] Passenger feedback system
- [ ] Advanced predictive analytics
- [ ] Multi-language support

---

**Made with ❤️ for the Matatu industry in Kenya**
