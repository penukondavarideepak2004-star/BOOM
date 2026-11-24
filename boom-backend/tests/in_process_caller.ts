import { EventEmitter } from "node:events";
import { Readable } from "node:stream";

export interface MockResponse {
  status: number;
  headers: Record<string, string>;
  body: any;
}

export function createInProcessCaller(requestListener: (req: any, res: any) => void) {
  return async function callApi(
    method: string,
    urlPath: string,
    body?: any,
    headers: Record<string, string> = {}
  ): Promise<MockResponse> {
    return new Promise((resolve, reject) => {
      // 1. Create Mock Request Stream
      const payload = body ? JSON.stringify(body) : "";
      const reqStream = new Readable({
        read() {
          if (payload) {
            this.push(Buffer.from(payload));
          }
          this.push(null);
        }
      });

      const normalizedHeaders: Record<string, string> = {
        host: "localhost",
        ...(body ? { "content-type": "application/json", "content-length": Buffer.byteLength(payload).toString() } : {})
      };
      for (const [k, v] of Object.entries(headers)) {
        normalizedHeaders[k.toLowerCase()] = v;
      }

      const req: any = Object.assign(reqStream, {
        method,
        url: urlPath,
        headers: normalizedHeaders,
        socket: {
          remoteAddress: "127.0.0.1"
        }
      });

      // 2. Create Mock Response Receiver
      let statusCode = 200;
      const resHeaders: Record<string, string> = {};
      let resBody = "";

      const res: any = new EventEmitter();
      res.setHeader = (key: string, val: string) => {
        resHeaders[key.toLowerCase()] = val;
      };
      res.getHeader = (key: string) => resHeaders[key.toLowerCase()];
      res.writeHead = (status: number, headersObj?: Record<string, string>) => {
        statusCode = status;
        if (headersObj) {
          for (const [k, v] of Object.entries(headersObj)) {
            resHeaders[k.toLowerCase()] = v;
          }
        }
      };
      res.write = (chunk: any) => {
        resBody += chunk?.toString() || "";
      };
      res.end = (chunk?: any) => {
        if (chunk) resBody += chunk.toString();
        let parsed = null;
        try {
          parsed = JSON.parse(resBody);
        } catch {
          parsed = resBody;
        }
        resolve({
          status: statusCode,
          headers: resHeaders,
          body: parsed
        });
      };

      try {
        requestListener(req, res);
      } catch (err) {
        reject(err);
      }
    });
  };
}
