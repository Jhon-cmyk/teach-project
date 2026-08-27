export class RTCPlayer {
  playerType: number
  videoSize: Record<string, string>
  container: Element | null

  on(event: string, handler: (...args: any[]) => void): RTCPlayer
  once?(event: string, handler: (...args: any[]) => void): RTCPlayer
  off?(event: string, handler?: (...args: any[]) => void): RTCPlayer

  play(): Promise<void> | void
  stop(): void
  resume?(): void
  destroy?(): void
}
