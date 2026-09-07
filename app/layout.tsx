import type {Metadata} from 'next';
import './globals.css';
export const metadata:Metadata={title:'Senda · Tu año en la Palabra',description:'Tu plan personal de lectura bíblica anual con enfoque temático y descargas trimestrales.'};
export default function Layout({children}:{children:React.ReactNode}){return <html lang="es"><body>{children}</body></html>}
