import React from 'react'
import { fetchData } from '../helper'
export function dashboardLoader() {
    const username = fetchData("userName");
    return{username}
}
const Dashboard = () => {
  return (
    <div>Dashboard</div>
  )
}

export default Dashboard