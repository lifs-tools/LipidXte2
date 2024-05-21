from distutils.core import setup, Extension

module1 = Extension("calcsf", sources=["calcsf.c"])

# setup (name = 'SFPack',
# 		version = '1.0',
# 		description = 'This is a testing package',
# 		ext_modules = [module1])

setup(
    name="Simple",
    version="1.0",
    description="This is a testing package",
    ext_modules=[module1],
)
