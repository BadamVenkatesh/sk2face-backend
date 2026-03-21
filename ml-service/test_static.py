import os

directory = "/home/badam/Downloads/tempi/ml_service/data"
path = "photos/m1-004-01.jpg"

joined_path = os.path.join(directory, path)
full_path = os.path.realpath(joined_path)
real_dir = os.path.realpath(directory)

print("joined_path:", joined_path)
print("full_path:", full_path)
print("real_dir:", real_dir)
print("commonpath:", os.path.commonpath([full_path, real_dir]))
print("matches:", os.path.commonpath([full_path, real_dir]) == str(real_dir))

try:
    print("stat full_path:", os.stat(full_path))
except Exception as e:
    print("stat error:", e)

try:
    print("stat joined_path:", os.stat(joined_path))
except Exception as e:
    print("stat error:", e)
