import { createBrowserRouter, Navigate } from 'react-router-dom'
import { App } from './App'
import { AssetPage } from './pages/AssetPage'
import { HistoryPage } from './pages/HistoryPage'
import { InvestPage } from './pages/InvestPage'
import { MarketPage } from './pages/MarketPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Navigate to="/invest" replace /> },
      { path: 'invest', element: <InvestPage /> },
      { path: 'market', element: <MarketPage /> },
      { path: 'assets', element: <AssetPage /> },
      { path: 'history', element: <HistoryPage /> },
      // 없는 경로는 주문 화면으로 되돌린다.
      { path: '*', element: <Navigate to="/invest" replace /> },
    ],
  },
])
