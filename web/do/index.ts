import type { ExecutionContext, DurableObject } from '@cloudflare/workers-types/2022-11-30'
import type { Env } from "./worker-configuration";

type Connector = {
  server: WebSocket
  sockets: WebSocket[]
  secret: string
}

export class WebSocketGateway implements DurableObject {
  sessions: Map<string, Connector | undefined> = new Map()

  async fetch(request: Request, env: Env, context: ExecutionContext) {
    const url = new URL(request.url)
    const id = url.pathname.split('/').pop()
    const secret = request.headers.get('x-forest-secret') ?? request.headers.get('sec-websocket-protocol').replace('forest', '').replace(',','').trim()

    console.log(`Connection from ${id} ${secret.replaceAll(/[^a-zA-Z0-9]/g, '')} ${url} ${request.headers.get('user-agent')}`)
    console.log(`Id ${request.headers.get('cf-ray')} ${request.headers.get('cf-connecting-ip')} ${request.headers.get('cf-ipcountry')}`)

    if (!secret) {
      return new Response('missing secret', {
        status: 400
      })
    }
    let session = this.sessions.get(id)

    if (!session && !request.headers.get('user-agent')?.includes('Forest')) {
      return new Response('missing server session', {
        status: 400
      })
    }

    const [client, server]: [WebSocket, WebSocket] = Object.values(new WebSocketPair())
    if (!session) {
      session = {
        server: client,
        sockets: [],
        secret
      }
      this.sessions.set(id, session)
      server.accept()

      server.addEventListener('message', event => {
        console.log(event)
        session?.sockets.forEach(socket => socket.send(event.data))
      })

      server.addEventListener('error', event => {
        console.error(event)
        session?.sockets.forEach(socket => socket.close(1000))
        this.sessions.delete(id)
      })

      server.addEventListener('close', event => {
        console.log(event)
        session?.sockets.forEach(socket => socket.close(1000))
        this.sessions.delete(id)
      })

      return new Response(null, {
        status: 101,
        webSocket: client
      } as ResponseInit)
    }

    // Client connection

    if (session.secret !== secret) {
      return new Response('invalid secret', {
        status: 403
      })
    }

    session.sockets.push(client)

    client.addEventListener('message', event => {
      console.log(event)
      session?.sockets.forEach(socket => socket.send(event.data))
    })

    client.addEventListener('error', event => {
      console.error(event)
    })

    client.addEventListener('close', event => {
      console.log(event)
      if (session?.sockets.length === 0) {
        session.server.close()
        this.sessions.delete(id)0
      } else {
        session && (session.sockets = session?.sockets.filter(socket => socket !== client))
      }
    })

    return new Response(null, {
      status: 101,
      webSocket: server
    } as ResponseInit)
  }
}

export default {}
