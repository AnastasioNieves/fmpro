import { initializeApp } from "firebase/app";

const firebaseConfig = {
  apiKey: "AIzaSyDBWUTxKlVE-UFD7xkHINxHceqFthL4NKU",
  authDomain: "fmpro-b4b7e.firebaseapp.com",
  projectId: "fmpro-b4b7e",
  storageBucket: "fmpro-b4b7e.firebasestorage.app",
  messagingSenderId: "505334758312",
  appId: "1:505334758312:web:ec97786030fe849ebefb9d"
};

import { getAuth } from "firebase/auth";

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

export { app, auth };
