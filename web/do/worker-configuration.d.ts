import { DurableObjectNamespace } from '@cloudflare/workers-types/2022-11-30'

interface Env {
	WS_GATEWAY: DurableObjectNamespace; 
}
