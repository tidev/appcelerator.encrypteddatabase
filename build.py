#!/usr/bin/python
import os, sys
import shutil, glob, platform, subprocess, string

def die(msg):
    print
    print "!!!!!!! ERROR !!!!!!!"
    print msg
    print "!!!!!!! ERROR !!!!!!!"
    if os.path.exists('temp.zip'):
        os.remove('temp.zip')
    sys.exit(1)

def fork(directory, cmd, quiet=False):
    proc = subprocess.Popen(cmd, shell=True, cwd=directory, stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
    while proc.poll() == None:
        line = proc.stdout.readline()
        if line and not quiet:
            print line.strip()
            sys.stdout.flush()
    return proc.returncode

def create_module(platform, cmd):
    build_path = os.path.join(os.getcwd(), platform)
    retcode = fork(build_path, cmd, False)
    if retcode == 0:
        print "Created %s module project" % platform
    else:
        die("Aborting")

def clean_build_module(platform):
    build_path = os.path.join(os.getcwd(), platform, 'build')
    if os.path.exists(build_path):
        shutil.rmtree(build_path)
    zip_file = os.path.join(os.getcwd(), platform, '*.zip')
    for fl in glob.glob(zip_file):
        os.remove(fl)
    print "Cleaned %s module project" % platform

def clean_ant_module(platform):
    build_path = os.path.join(os.getcwd(), platform, 'build')
    if os.path.exists(build_path):
        shutil.rmtree(build_path)
    ant_path = os.path.join(os.getcwd(), platform)
    retcode = fork(ant_path, 'ant clean', False)
    retcode = fork(ant_path, 'ant cleanlinks', False)
    zip_file = os.path.join(os.getcwd(), platform, 'dist', '*.zip')
    for fl in glob.glob(zip_file):
        os.remove(fl)
    print "Cleaned %s module project" % platform

def main(args):
    print "Appcelerator Titanium Module Builder"
    print

    if len(args) < 2:
        cmd = 'build'
    elif args[1] == 'clean':
        cmd = 'clean'
    else:
        die("Invalid command")

    if cmd == 'build':
        packages = []
        if os.path.exists('iphone'):
            create_module('iphone', './build.py')
            packages.append('iphone')

        if os.path.exists('mobileweb'):
            create_module('mobileweb', './build.py')
            packages.append('mobileweb')

        if os.path.exists('android'):
            if not os.path.exists(os.path.join(os.getcwd(), 'android', 'build.properties')):
                die("*** build.properties file is missing -- copy build.properties.example ***")
            create_module('android', 'ant')
            packages.append('android')

        packages_cmd = './package.py --platform=' + string.join(packages, ',')
        fork('.', packages_cmd, False)

    elif cmd == 'clean':
        if os.path.exists('iphone'):
            clean_build_module('iphone')

        if os.path.exists('mobileweb'):
            clean_build_module('mobileweb')

        if os.path.exists('android'):
            clean_ant_module('android')

        if os.path.exists('commonjs'):
            clean_build_module('commonjs')

        zip_file = os.path.join(os.getcwd(), '*.zip')
        for fl in glob.glob(zip_file):
            os.remove(fl)

if __name__ == "__main__":
    main(sys.argv)