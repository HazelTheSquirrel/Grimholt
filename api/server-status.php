<?php
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate');
$configPath=dirname(__DIR__).'/data/server.json';
try{$config=json_decode((string)file_get_contents($configPath),true,512,JSON_THROW_ON_ERROR);}catch(Throwable){http_response_code(500);echo json_encode(['online'=>false,'enabled'=>false,'error'=>'Serverkonfiguration fehlt.'],JSON_UNESCAPED_UNICODE);exit;}
$host=(string)($config['host']??'127.0.0.1');$port=(int)($config['port']??25565);$enabled=(bool)($config['enabled']??false);$timeoutMs=max(250,min(3000,(int)($config['query_timeout_ms']??1200)));
$result=['online'=>false,'enabled'=>$enabled,'name'=>(string)($config['name']??'Grimholt'),'host'=>$host,'port'=>$port,'players'=>['online'=>0,'max'=>0],'version'=>(string)($config['version']??''),'motd'=>(string)($config['motd']??'')];
if(!$enabled){echo json_encode($result,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);exit;}
$socket=@fsockopen($host,$port,$errno,$errstr,$timeoutMs/1000);
if(is_resource($socket)){stream_set_timeout($socket,0,$timeoutMs*1000);fclose($socket);$result['online']=true;}
echo json_encode($result,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);
