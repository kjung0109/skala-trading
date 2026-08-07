import { useQuery } from '@tanstack/react-query'
import { auditApi } from '../../../api/endpoints'
import { cn } from '../../../lib/cn'
import { num, timeOf } from '../../../lib/format'
import { qk } from '../../../lib/queryClient'
import { PanelBody, PanelEmpty, Th } from './PanelTable'

/**
 * AOP가 남긴 주문 처리 기록.
 *
 * 거절된 주문은 롤백되어 주문 내역에는 남지 않는다.
 * 무엇을 시도했다가 왜 안 됐는지는 여기서만 볼 수 있다.
 */
export function AuditTable() {
  const { data } = useQuery({ queryKey: qk.myAudit, queryFn: auditApi.myLogs })

  const rows = data?.list ?? []
  if (rows.length === 0) {
    return (
      <PanelEmpty
        message="처리 기록이 없습니다"
        sub="주문하거나 취소하면 성공·실패가 모두 기록됩니다"
      />
    )
  }

  return (
    <PanelBody>
      <table className="w-full text-[11px] tnum">
        <thead className="sticky top-0 z-10 bg-surface-subtle text-foreground-disabled">
          <tr className="border-b border-stroke-subtle">
            <Th align="left">시각</Th>
            <Th>동작</Th>
            <Th align="left">요청 내용</Th>
            <Th>결과</Th>
            <Th align="left">메시지</Th>
            <Th align="right">소요</Th>
          </tr>
        </thead>
        <tbody>
          {rows.map((log) => (
            <tr key={log.logId} className="border-b border-stroke-subtle last:border-0">
              <td className="py-[5px] pl-3 font-semibold text-foreground-tertiary">
                {timeOf(log.createdAt)}
              </td>
              <td className="py-[5px] text-center font-bold text-foreground-secondary">
                {log.action === 'PLACE' ? '주문' : '취소'}
              </td>
              <td className="py-[5px] font-semibold text-foreground">{log.detail}</td>
              <td className="py-[5px] text-center">
                <span
                  className={cn(
                    'inline-block rounded border px-1 py-px text-[10px] font-bold',
                    log.success
                      ? 'border-stroke bg-surface-muted text-foreground-secondary'
                      : 'border-up-border bg-up-bg text-up',
                  )}
                >
                  {log.success ? '성공' : '거절'}
                </span>
              </td>
              <td
                className={cn(
                  'py-[5px] font-semibold',
                  log.success ? 'text-foreground-tertiary' : 'text-up',
                )}
              >
                {log.message}
              </td>
              <td className="py-[5px] pr-3 text-right font-semibold text-foreground-tertiary">
                {num(log.elapsedMs)}ms
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </PanelBody>
  )
}
