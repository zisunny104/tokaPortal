import argparse, pathlib, shutil, subprocess, uuid, zipfile, json

p=argparse.ArgumentParser();p.add_argument('--core',required=True);p.add_argument('--jdk',required=True);p.add_argument('--jar',required=True);p.add_argument('--cache',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];core=pathlib.Path(a.core).resolve();jar=pathlib.Path(a.jar).resolve();jdk=pathlib.Path(a.jdk)
run=root/'.build-tools'/('storage-boot-'+uuid.uuid4().hex[:12]);run.mkdir(parents=True)
classes=run/'probe';launcher=run/'launcher';classes.mkdir();launcher.mkdir()
subprocess.run([str(jdk/'bin/javac.exe'),'-encoding','UTF-8','-proc:none','-cp',str(core)+';'+str(jar),'-d',str(classes),str(root/'tests/smoke/StorageProbe.java')],check=True)
subprocess.run([str(jdk/'bin/javac.exe'),'-proc:none','-cp',str(core),'-d',str(launcher),str(root/'tests/smoke/StorageLauncher.java')],check=True)
results=[]
for backend in ('sqlite','json','yaml'):
    work=run/backend;(work/'plugins/tokaPortal').mkdir(parents=True)
    shutil.copy2(jar,work/'plugins/tokaPortal.jar')
    (work/'plugins/tokaPortal/config.yml').write_text('storage:\n  backend: '+backend+'\nmetrics:\n  enabled: false\n',encoding='utf-8')
    with zipfile.ZipFile(work/'plugins/StorageProbe.jar','w') as z:
        for file in classes.rglob('*.class'):z.write(file,file.relative_to(classes).as_posix())
        z.writestr('plugin.yml','name: StorageProbe\nmain: StorageProbe\nversion: "0.1"\napi: ["1.0.4"]\nload: POSTWORLD\nsoftdepend: [tokaPortal]\n')
    for name in ('discovery-cache.json','openid-cache.json'):shutil.copy2(pathlib.Path(a.cache)/name,work/name)
    (work/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=19244\nlevel-name=test\nlevel-type=flat\nxbox-auth=off\nquery=off\nenable-rcon=off\nauto-save=off\nload-all-worlds=off\nallow-nether=off\nallow-end=off\n',encoding='utf-8')
    log=work/'console.log'
    with log.open('w',encoding='utf-8') as output:
        proc=subprocess.Popen([str(jdk/'bin/java.exe'),'-Xms128m','-Xmx512m','-Dfile.encoding=UTF-8','-Dprobe.backend='+backend,'-cp',str(launcher)+';'+str(core),'StorageLauncher'],cwd=work,stdout=output,stderr=subprocess.STDOUT)
        try:code=proc.wait(timeout=60)
        except subprocess.TimeoutExpired:proc.kill();proc.wait();raise RuntimeError('Core timeout '+str(log))
    text=log.read_text(encoding='utf-8',errors='replace');passed=('PORTAL_STORAGE_BOOT_PASS Backend='+backend) in text and 'PORTAL_STORAGE_BOOT_FAIL' not in text and code==0
    results.append(dict(backend=backend,passed=passed,log=str(log)));print(backend, 'PASS' if passed else 'FAIL',log,flush=True)
    if not passed:raise RuntimeError('Core check failed: '+str(log))
(run/'result.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
print('ALL_PORTAL_STORAGE_BOOT_PASS',run,flush=True)
