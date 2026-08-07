import { useEffect, useState } from 'react'
import { SectionTabs } from '../ui/SectionTabs'
import { AuditTable } from './panels/AuditTable'
import { HoldingTable } from './panels/HoldingTable'
import { MyTradesTable } from './panels/MyTradesTable'
import { PendingOrdersTable } from './panels/PendingOrdersTable'
import { TradeTapeTable } from './panels/TradeTapeTable'

type LeftTab = 'tape' | 'audit'
type RightTab = 'trades' | 'pending' | 'holding'

const LEFT_TABS = [
  { key: 'tape' as const, label: '실시간 체결' },
  { key: 'audit' as const, label: '처리 기록' },
]

const RIGHT_TABS = [
  { key: 'trades' as const, label: '내 체결' },
  { key: 'pending' as const, label: '미체결' },
  { key: 'holding' as const, label: '보유 종목' },
]

const LEFT_KEY = 'skala-trading.invest.leftTab'
const RIGHT_KEY = 'skala-trading.invest.rightTab'

type Props = { stockId: number }

/**
 * 주문 화면 하단.
 *
 * 왼쪽은 시장이 어떻게 움직이는지(모두에게 공개된 정보),
 * 오른쪽은 내 계좌가 어떤 상태인지를 본다.
 * 주문을 넣고 결과를 확인하려고 화면을 옮겨 다닐 필요가 없어야 한다.
 *
 * 고른 탭은 저장해 둔다. 새로고침할 때마다 원래 보던 탭으로 돌아가면 번거롭다.
 */
export function BottomPanels({ stockId }: Props) {
  const [leftTab, setLeftTab] = useState<LeftTab>(
    () => (localStorage.getItem(LEFT_KEY) as LeftTab) ?? 'tape',
  )
  const [rightTab, setRightTab] = useState<RightTab>(
    () => (localStorage.getItem(RIGHT_KEY) as RightTab) ?? 'trades',
  )

  useEffect(() => localStorage.setItem(LEFT_KEY, leftTab), [leftTab])
  useEffect(() => localStorage.setItem(RIGHT_KEY, rightTab), [rightTab])

  return (
    <div className="flex shrink-0 flex-col overflow-hidden border-t-2 border-stroke bg-surface xl:h-[224px] xl:flex-row">
      <section className="flex h-[224px] min-w-0 flex-col overflow-hidden border-stroke xl:h-auto xl:flex-1 xl:border-r">
        <SectionTabs items={LEFT_TABS} activeKey={leftTab} onChange={setLeftTab} />
        {leftTab === 'tape' && <TradeTapeTable stockId={stockId} />}
        {leftTab === 'audit' && <AuditTable />}
      </section>

      <section className="flex h-[224px] min-w-0 flex-col overflow-hidden border-t border-stroke xl:h-auto xl:flex-1 xl:border-t-0">
        <SectionTabs items={RIGHT_TABS} activeKey={rightTab} onChange={setRightTab} />
        {rightTab === 'trades' && <MyTradesTable />}
        {rightTab === 'pending' && <PendingOrdersTable />}
        {rightTab === 'holding' && <HoldingTable />}
      </section>
    </div>
  )
}
