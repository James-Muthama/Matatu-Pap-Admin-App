# Matatu Pap Admin App

**Matatu Pap Admin App** is a mobile application designed to streamline the management of *matatus* (public transport buses in Kenya). The app allows administrators to track matatus, add buses, define routes using Google Maps, and connect buses to specific routes. It simplifies route creation by enabling users to add stages (stops) along a route, leveraging Google Maps for ease of use.

## Features

- **Add a Matatu**: Register and manage details of matatus, including vehicle information.
- **Route Management**: Create and manage routes by adding stages (stops) using an intuitive Google Maps integration.
- **Connect Matatus to Routes**: Assign matatus to specific routes for efficient tracking and management.
- **User-Friendly Interface**: Designed for administrators to easily manage matatu operations.

## Getting Started

### Prerequisites
- **Android Studio**: Required for building and running the app.
- **Git**: Ensure Git is installed for cloning the repository.
- **Google Maps API Key**: Obtain a Google Maps API key from the [Google Cloud Console](https://cloud.google.com/maps-platform) to enable map functionality.
- **Firebase (Optional)**: If the app uses Firebase for backend services, set up a Firebase project.

### Installation

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/James-Muthama/Matatu-Pap-Admin-App.git

2. **Installation**:
#### Open in Android Studio
- Launch Android Studio.
- Select `File` > `Open` and navigate to the cloned `Matatu-Pap-Admin-App` folder.
- Let Android Studio sync the project with Gradle.

#### Configure Google Maps API
- Add your Google Maps API key to the `google_maps_api.xml` file (typically in `app/src/main/res/values/`):
- Replace YOUR_API_KEY with your actual Google Maps API key.

#### Build and Run
- Connect an Android device or use an emulator.
- Click Run in Android Studio to build and deploy the app.

## Usage
1. **Clone the Repository**:
- Navigate to the "Add Matatu" section in the app.
- Enter details such as matatu name, registration number, and other relevant information.

2. **Create a Route:**:
- Go to the "Add Route" section.
- Use the Google Maps interface to select stages (stops) along the route.
- Save the route to store it in the app.

3. **Connect Matatu to Route:**:
- GitHub Repository: Matatu Pap Admin App

## Instructions for Use
1. Copy the above Markdown content.
2. Go to your GitHub repository: [https://github.com/James-Muthama/Matatu-Pap-Admin-App](https://github.com/James-Muthama/Matatu-Pap-Admin-App).
3. Click on `README.md` (or create it if it doesn’t exist by clicking `Add file` > `Create new file` and naming it `README.md`).
4. Paste the content into the file.
5. Commit the changes with a message like "Add README file" and save.

### Notes
- **Placeholder Image**: The `![Matatu Pap Admin App](https://via.placeholder.com/150)` is a placeholder. Replace it with a URL to an actual app logo or screenshot if available (e.g., upload an image to the repository and link it).
- **License**: The README assumes an MIT License. If you’re using a different license, update the `License` section and ensure a `LICENSE` file exists in the repository.
- **Firebase**: The setup mentions Firebase as optional since many mobile apps use it. If your app doesn’t use Firebase, you can remove that part.

