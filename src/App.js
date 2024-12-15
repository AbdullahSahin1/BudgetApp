import './App.css';
import {createBrowserRouter,RouterProvider} from "react-router-dom"
import Error from './pages/Error'
import Dashboard, {dashboardLoader} from './pages/Dashboard';
const router = createBrowserRouter([
  {
    path: "/",
    element:<Dashboard/>,
    loader: dashboardLoader,
    errorElement: <Error/>
  }
])
function App() {
  return (
    <div className="App">
      <RouterProvider router={router}/>
    </div>
  );
}

export default App;
