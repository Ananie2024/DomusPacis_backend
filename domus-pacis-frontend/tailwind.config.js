/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/**/*.{js,ts,jsx,tsx}',
    './pages/**/*.{js,ts,jsx,tsx}',
    './components/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        gold: {
          50: '#fefce8',
          100: '#fef9c3',
          200: '#fef08a',
          300: '#fde047',
          400: '#facc15',
          500: '#eab308',
          600: '#ca8a04',
          700: '#b8860b',
          800: '#78350f',
          900: '#422006',
        },
        burgundy: {
          50: '#fdf6f7',
          100: '#f9e6e8',
          200: '#f2c7cd',
          300: '#e697a0',
          400: '#d46b76',
          500: '#c44578',
          600: '#a42a5f',
          700: '#8a1f4e',
          800: '#5f1a38',
          900: '#3d1325',
          950: '#2a0e19',
        },
      },
      fontFamily: {
        display: ['Playfair Display', 'Georgia', 'serif'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
