#!/usr/bin/env python
import os, sys, glob, zipfile, re, uuid, types

# OUTPUT FILENAME MUST BE <modulename>-*-* TO BE AUTO-UNZIPPED BY TITANIUM

def die(msg):
    print
    print "!!!!!!! ERROR !!!!!!!"
    print msg
    print "!!!!!!! ERROR !!!!!!!"
    if os.path.exists('temp.zip'):
        os.remove('temp.zip')
    sys.exit(1)

def parse_manifest(contents):
    dict = {}
    for line in contents.splitlines(True):
        if line[0:1]=='#': continue
        idx = line.find(':')
        if idx==-1: continue
        k=line[0:idx]
        v=line[idx+1:].strip()
        dict[k]=v
    return dict

def check_file(args,zin):
    for  name in zin.namelist():
        if name.endswith('manifest'):
            contents = zin.read(name)
            manifest = parse_manifest(contents)
            if has_config(args, 'version'):
                if manifest['version'] != args['version']:
                    die("Module versions do not match")
            else:
                args['version'] = manifest['version']
            if has_config(args, 'guid'):
                if manifest['guid'] != args['guid']:
                    die("Module guids do not match")
            else:
                args['guid'] = manifest['guid']

def move_zip(zin, zout):
    for name in zin.namelist():
    	if name.endswith('/'):
    		zif = zipfile.ZipInfo(name)
    		zif.external_attr = 040755 << 16L # permissions drwxr-xr-x
    		zout.writestr(zif, '')
    	else:
        	contents = zin.read(name)
        	zout.writestr(name, contents)

def package_modules(args):
    if os.path.exists('temp.zip'):
        os.remove('temp.zip')

    zout = zipfile.ZipFile('temp.zip', 'w', zipfile.ZIP_DEFLATED)
    modlist = get_required(args,'modlist')
    for src in modlist:
        print 'Packaging ' + src
        zin = zipfile.ZipFile(src,'r')
        check_file(args,zin)
        move_zip(zin,zout)
        zin.close()
    zout.close()

    tgtfilename = args['modname'] + "-titanium-" + args['version'] + ".zip";
    tgtfile = os.path.join(os.getcwd(), tgtfilename)
    if os.path.exists(tgtfile):
        os.remove(tgtfile)
    os.rename('temp.zip',tgtfile)

    print
    print 'Packaged modules into ' + tgtfile

def select_module(srcfolder, modname, platform, modlist):
    version = ""
    subfolder = ""
    if platform == 'android':
        subfolder = 'dist'
    prefix = os.path.join(srcfolder, platform, subfolder, modname + "-" + platform + "-")
    for filename in glob.glob(prefix + "*"):
        modlist.append(filename)
        version = filename.split("-")[2].replace(".zip","")
    return version

def has_config(config,key,has_value=True):
    if not config.has_key(key) or (has_value and config[key]==None):
        return False
    return True

def get_optional(config,key,default=None):
    if not has_config(config,key,False):
        return default
    value = config[key]
    if value == None:
        return default
    return value

def get_required(config, key, env=None):
    if not has_config(config, key):
        if env and env in os.environ: return os.environ[env]
        if env == None:
            die("required argument '--%s' missing" % key)
        else:
            die("required argument '--%s' missing (you can also set the environment variable %s)" % (key, env))
    return config[key]

def select_modules(args):
    project_dir = get_required(args,'dir')
    platform = get_optional(args,'platform')
    modname = get_required(args,'modname')
    modlist = get_required(args,'modlist')
    if type(platform) == types.NoneType:
        select_module(project_dir, modname, 'iphone', modlist)
        select_module(project_dir, modname, 'android', modlist)
        select_module(project_dir, modname, 'mobileweb', modlist)
        select_module(project_dir, modname, 'commonjs', modlist)
    elif type(platform) == types.ListType:
        for osname in platform:
            select_module(project_dir, modname, osname, modlist)
    else:
        select_module(project_dir, modname, platform, modlist)

def slurp_args(args):
    config = {"args": []}
    for arg in args:
        if arg[0:2]=='--':
            arg = arg[2:]
            idx = arg.find('=')
            k = arg
            v = None
            if idx>0:
                k=arg[0:idx]
                v=arg[idx+1:]
            if v!=None and v.find(',')!=-1:
                v = v.split(',')
            config[k]=v
        else:
            config["args"].append(arg)
    return config

def help(args=[],suppress_banner=False):
    print "Appcelerator Titanium Module Packager"
    print "Copyright (c) 2010-2013 by Appcelerator, Inc."
    print

    if len(args)==0:
        print "Usage: --platform=p1,p2     platform: iphone, android, mobileweb, commonjs"
        print "Example: ./package.py --platform=iphone,android,mobileweb"
    print
    sys.exit(-1)

def main(args):
    if len(args) < 2:
        help()

    print "Appcelerator Titanium Module Packager"
    print

    # convert args to a hash
    command = args[1]
    a = list(args)
    a.pop(0) # program

    config = slurp_args(a)
    config['dir']=os.getcwd()
    config['modname']=os.path.basename(os.getcwd())
    config['modlist']=[]

    select_modules(config)

    if (len(config['modlist']) > 0):
        package_modules(config)

if __name__ == "__main__":
    main(sys.argv)
