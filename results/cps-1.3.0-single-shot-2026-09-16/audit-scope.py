from pathlib import Path
import hashlib,json,subprocess,sys
base=Path('/Users/karenbarseghyan/Projects/j-util')
evidence=Path(sys.argv[1])
before=json.loads((evidence/'preflight.json').read_text())
result={}
for name in ['columnar-projection-store','columnar-projection-store-hardwood']:
 repo=base/name
 sha=subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip()
 status=subprocess.check_output(['git','status','--porcelain=v1'],cwd=repo,text=True)
 changes=[path for path,digest in before[name]['files'].items() if hashlib.sha256((repo/path).read_bytes()).hexdigest()!=digest]
 assert sha==before[name]['sha'] and not status and not changes,(name,sha,status,changes)
 result[name]={'sha':sha,'status':status,'changed_tracked_files':changes,'checked_files':len(before[name]['files'])}
repo=base/'performance-lab'
original='results/cps-1.3.0-2026-09-16/'
paths=[p for p in before['performance-lab']['files'] if p.startswith(original) and p!=original+'README.md']
assert all(hashlib.sha256((repo/path).read_bytes()).hexdigest()==before['performance-lab']['files'][path] for path in paths)
old=subprocess.check_output(['git','show','389be7e3ca1c77c1bc1ba5eac055fe1f967e542f:'+original+'README.md'],cwd=repo,text=True)
current=(repo/original/'README.md').read_text()
assert current.startswith(old.split('\n',1)[0]) and current.endswith(old.split('\n',1)[1])
subprocess.run(['git','diff','--exit-code','41afe3444622e9a46d3f0bf2d9f5e9d2e6fadcee','--','benchmark-core','benchmark-jmh','scripts/cps13'],cwd=repo,check=True)
result['performance-lab']={'measured_source_sha':'41afe3444622e9a46d3f0bf2d9f5e9d2e6fadcee','benchmark_and_scripts_unchanged':True,'historical_raw_files_unchanged':paths,'original_report_body_preserved':True}
(evidence/'scope-audit.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS: CPS and Hardwood SHAs, all tracked files and clean worktrees unchanged; historical raw evidence and report body preserved; measured benchmark sources unchanged.')
