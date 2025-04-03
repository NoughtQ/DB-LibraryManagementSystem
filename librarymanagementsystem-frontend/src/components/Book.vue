<template>
    <el-scrollbar height="100%" style="width: 100%;">
        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理
            <el-input v-model="toSearch" :prefix-icon="Search"
                style="width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 42px; float: right;" clearable
                placeholder="搜索书名或作者" />
            <el-button type="info" @click="openAdvancedSearchDialog" style="float: right; margin-right: 10px; margin-left: 10px">
                <el-icon>
                    <Search />
                </el-icon>
                高级查询
            </el-button>
            <el-button type="warning" @click="openBatchImportDialog" style="float: right; margin-right: 10px;">
                <el-icon>
                    <Upload />
                </el-icon>
                批量导入
            </el-button>
            <el-button type="primary" @click="openNewBookDialog" style="float: right; margin-right: 10px;">
                <el-icon>
                    <Plus />
                </el-icon>
                添加图书
            </el-button>
        </div>

        <!-- 图书表格 -->
        <div style="margin: 20px 40px;">
            <el-table :data="filteredBooks" style="width: 100%" border stripe>
                <el-table-column prop="bookId" label="ID" width="80" sortable />
                <el-table-column prop="title" label="书名" min-width="150" sortable />
                <el-table-column prop="author" label="作者" min-width="100" sortable />
                <el-table-column prop="category" label="分类" width="120" sortable />
                <el-table-column prop="press" label="出版社" min-width="120" sortable />
                <el-table-column prop="publishYear" label="出版年份" width="120" sortable />
                <el-table-column prop="price" label="价格" width="100" sortable>
                    <template #default="scope">
                        ¥{{ scope.row.price }}
                    </template>
                </el-table-column>
                <el-table-column prop="stock" label="库存" width="100" sortable />
                <el-table-column label="操作" width="230" fixed="right">
                    <template #default="scope">
                        <el-button type="primary" size="small" @click="openModifyDialog(scope.row)">
                            <el-icon>
                                <Edit />
                            </el-icon>
                            修改
                        </el-button>
                        <el-button type="warning" size="small" @click="openModifyStockDialog(scope.row)">
                            <el-icon>
                                <Plus />
                            </el-icon>
                            库存
                        </el-button>
                        <el-button type="danger" size="small" @click="openRemoveDialog(scope.row.bookId)">
                            <el-icon>
                                <Delete />
                            </el-icon>
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 高级查询对话框 -->
        <el-dialog v-model="advancedSearchVisible" title="高级查询" width="40%" align-center>
            <div style="margin-left: 2vw; font-size: 1rem; margin-top: 20px;">
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">书名：</span>
                    <el-input v-model="searchConditions.title" style="width: 20vw; margin-left: 32px;" clearable placeholder="模糊查询" />
                </div>
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">作者：</span>
                    <el-input v-model="searchConditions.author" style="width: 20vw; margin-left: 32px;" clearable placeholder="模糊查询" />
                </div>
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">分类：</span>
                    <el-input v-model="searchConditions.category" style="width: 20vw; margin-left: 32px;" clearable placeholder="精确查询" />
                </div>
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">出版社：</span>
                    <el-input v-model="searchConditions.press" style="width: 20vw; margin-left: 17px;" clearable placeholder="模糊查询" />
                </div>
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">出版年份：</span>
                    <el-input v-model="searchConditions.minPublishYear" style="width: 9vw;" clearable placeholder="最小值" />
                    <span style="margin: 0 10px;">至</span>
                    <el-input v-model="searchConditions.maxPublishYear" style="width: 9vw;" clearable placeholder="最大值" />
                </div>
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">价格：</span>
                    <el-input v-model="searchConditions.minPrice" style="width: 9vw; margin-left: 32px;" clearable placeholder="最小值" />
                    <span style="margin: 0 10px;">至</span>
                    <el-input v-model="searchConditions.maxPrice" style="width: 9vw;" clearable placeholder="最大值" />
                </div>
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">排序字段：</span>
                    <el-select v-model="searchConditions.sortBy" style="width: 20vw;">
                        <el-option label="图书ID" value="book_id" />
                        <el-option label="书名" value="title" />
                        <el-option label="作者" value="author" />
                        <el-option label="分类" value="category" />
                        <el-option label="出版社" value="press" />
                        <el-option label="出版年份" value="publish_year" />
                        <el-option label="价格" value="price" />
                        <el-option label="库存" value="stock" />
                    </el-select>
                </div>
                <div style="margin-bottom: 15px;">
                    <span style="font-weight: bold;">排序方式：</span>
                    <el-select v-model="searchConditions.sortOrder" style="width: 20vw;">
                        <el-option label="升序" value="ASC" />
                        <el-option label="降序" value="DESC" />
                    </el-select>
                </div>
            </div>

            <template #footer>
                <span>
                    <el-button @click="resetSearchConditions">重置</el-button>
                    <el-button @click="advancedSearchVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmAdvancedSearch">查询</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 批量导入图书对话框 -->
        <el-dialog v-model="batchImportVisible" title="批量导入图书" width="40%" align-center>
            <div style="text-align: center; margin-bottom: 20px;">
                <p>请上传包含图书信息的CSV文件</p>
                <p style="color: #666; font-size: 12px;">CSV格式：标题,作者,分类,出版社,出版年份,价格,库存</p>
            </div>
            
            <el-upload
                class="upload-demo"
                drag
                action="#"
                :auto-upload="false"
                :on-change="handleFileChange"
                :limit="1"
                accept=".csv"
                style="width: 100%;"
            >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                    拖拽文件到此处或 <em>点击上传</em>
                </div>
                <template #tip>
                    <div class="el-upload__tip">
                        请上传CSV格式文件
                    </div>
                </template>
            </el-upload>

            <div v-if="csvPreview.length > 0" style="margin-top: 20px;">
                <h4>预览 (最多显示5条记录):</h4>
                <el-table :data="csvPreview.slice(0, 5)" border style="width: 100%">
                    <el-table-column prop="title" label="书名" width="120" />
                    <el-table-column prop="author" label="作者" width="100" />
                    <el-table-column prop="category" label="分类" width="80" />
                    <el-table-column prop="press" label="出版社" width="100" />
                    <el-table-column prop="publishYear" label="出版年份" width="80" />
                    <el-table-column prop="price" label="价格" width="70" />
                    <el-table-column prop="stock" label="库存" width="70" />
                </el-table>
                <div style="margin-top: 10px; color: #666;">
                    共 {{ csvPreview.length }} 条记录
                </div>
            </div>

            <template #footer>
                <span>
                    <el-button @click="batchImportVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmBatchImport" 
                        :disabled="csvPreview.length === 0">导入</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 新建图书对话框 -->
        <el-dialog v-model="newBookVisible" title="新建图书" width="40%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="newBookInfo.title" style="width: 20vw; margin-left: 32px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="newBookInfo.author" style="width: 20vw; margin-left: 32px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                分类：
                <el-input v-model="newBookInfo.category" style="width: 20vw; margin-left: 32px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="newBookInfo.press" style="width: 20vw; margin-left: 16px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input v-model="newBookInfo.publishYear" type="number" style="width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input v-model="newBookInfo.price" type="number" style="width: 20vw; margin-left: 32px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                库存：
                <el-input v-model="newBookInfo.stock" type="number" style="width: 20vw; margin-left: 32px;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmNewBook"
                        :disabled="!isNewBookFormValid">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 修改图书对话框 -->   
        <el-dialog v-model="modifyBookVisible" :title="'修改图书信息(ID: ' + toModifyInfo.bookId + ')'" width="40%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="toModifyInfo.title" style="width: 20vw; margin-left: 32px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="toModifyInfo.author" style="width: 20vw; margin-left: 32px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                分类：
                <el-input v-model="toModifyInfo.category" style="width: 20vw; margin-left: 32px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="toModifyInfo.press" style="width: 20vw; margin-left: 17px;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input v-model="toModifyInfo.publishYear" type="number" style="width: 20vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input v-model="toModifyInfo.price" type="number" style="width: 20vw; margin-left: 32px;" clearable />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmModifyBook"
                        :disabled="!isModifyFormValid">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 修改库存对话框 -->
        <el-dialog v-model="modifyStockVisible" :title="'修改库存(ID: ' + toModifyStockInfo.bookId + ')'" width="30%">
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                当前库存：{{ toModifyStockInfo.currentStock }}
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                增加/减少：
                <el-input-number v-model="toModifyStockInfo.deltaStock" :min="-toModifyStockInfo.currentStock" style="width: 12vw;" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyStockVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmModifyStock">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 删除图书对话框 -->  
        <el-dialog v-model="removeBookVisible" title="删除图书" width="30%">
            <span>确定删除<span style="font-weight: bold;">ID为{{ toRemove }}的图书</span>吗？</span>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisible = false">取消</el-button>
                    <el-button type="danger" @click="confirmRemoveBook">
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { Search, Plus, Edit, Delete, Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
    data() {
        return {
            books: [],                    // 图书列表
            Delete,
            Edit,
            Search,
            Plus,
            Upload,
            UploadFilled,
            toSearch: '',                  // 搜索内容
            newBookVisible: false,         // 新建图书对话框可见性
            removeBookVisible: false,      // 删除图书对话框可见性
            modifyBookVisible: false,      // 修改图书对话框可见性
            modifyStockVisible: false,     // 修改库存对话框可见性
            batchImportVisible: false,     // 批量导入对话框可见性
            csvPreview: [],                // CSV预览数据
            csvData: [],                   // 完整CSV数据
            toRemove: 0,                   // 待删除图书ID
            newBookInfo: {                 // 待新建图书信息
                title: '',
                author: '',
                category: '',
                press: '',
                publishYear: 2023,
                price: 0,
                stock: 0
            },
            toModifyInfo: {                // 待修改图书信息
                bookId: 0,
                title: '',
                author: '',
                category: '',
                press: '',
                publishYear: 0,
                price: 0
            },
            toModifyStockInfo: {           // 待修改库存信息
                bookId: 0,
                currentStock: 0,
                deltaStock: 0
            },
            
            // 添加高级搜索相关数据
            advancedSearchVisible: false,
            searchConditions: {
                category: '',
                title: '',
                press: '',
                minPublishYear: '',
                maxPublishYear: '',
                author: '',
                minPrice: '',
                maxPrice: '',
                sortBy: 'book_id',
                sortOrder: 'ASC'
            },
        }
    },
    computed: {
        isNewBookFormValid() {
            return this.newBookInfo.title.length > 0 && 
                   this.newBookInfo.author.length > 0 && 
                   this.newBookInfo.category.length > 0 && 
                   this.newBookInfo.press.length > 0 && 
                   this.newBookInfo.publishYear > 0 && 
                   this.newBookInfo.price >= 0 && 
                   this.newBookInfo.stock >= 0;
        },
        isModifyFormValid() {
            return this.toModifyInfo.title.length > 0 && 
                   this.toModifyInfo.author.length > 0 && 
                   this.toModifyInfo.category.length > 0 && 
                   this.toModifyInfo.press.length > 0 && 
                   this.toModifyInfo.publishYear > 0 && 
                   this.toModifyInfo.price >= 0;
        },
        filteredBooks() {
            if (!this.toSearch) {
                return this.books;
            }
            return this.books.filter(book => 
                book.title.toLowerCase().includes(this.toSearch.toLowerCase()) || 
                book.author.toLowerCase().includes(this.toSearch.toLowerCase())
            );
        }
    },
    methods: {
        // 图书入库对话框
        openNewBookDialog() {
            this.newBookInfo = {
                title: '',
                author: '',
                category: '',
                press: '',
                publishYear: 0,
                price: 0,
                stock: 0
            };
            this.newBookVisible = true;
        },

        // 图书信息修改对话框
        openModifyDialog(book) {
            this.toModifyInfo = {
                bookId: book.bookId,
                title: book.title,
                author: book.author,
                category: book.category,
                press: book.press,
                publishYear: book.publishYear,
                price: book.price
            };
            this.modifyBookVisible = true;
        },

        // 图书移除对话框
        openRemoveDialog(bookId) {
            this.toRemove = bookId;
            this.removeBookVisible = true;
        },

        // 图书库存修改对话框
        openModifyStockDialog(book) {
            this.toModifyStockInfo = {
                bookId: book.bookId,
                currentStock: book.stock,
                deltaStock: 0
            };
            this.modifyStockVisible = true;
        },

        // 图书入库处理函数
        confirmNewBook() {
            const bookToSend = {
                ...this.newBookInfo,
                publishYear: Number(this.newBookInfo.publishYear),
                price: Number(this.newBookInfo.price),
                stock: Number(this.newBookInfo.stock)
            };
            
            axios.post("/book", bookToSend)
                .then(response => {
                    ElMessage.success("图书新建成功");
                    this.newBookVisible = false;
                    this.queryBooks();
                })
                .catch(error => {
                    let errorMsg = "图书创建失败，原因：";
                    if (error.response && error.response.data) {
                        errorMsg += error.response.data;
                    }
                    console.error("创建图书错误详情:", error);
                    ElMessage.error(errorMsg);
                });
        },

        // 图书信息修改处理函数
        confirmModifyBook() {
            const bookToSend = {
                ...this.toModifyInfo,
                bookId: Number(this.toModifyInfo.bookId),
                publishYear: Number(this.toModifyInfo.publishYear),
                price: Number(this.toModifyInfo.price)
            };
            
            axios.put("/book", bookToSend)
                .then(response => {
                    ElMessage.success("图书信息修改成功");
                    this.modifyBookVisible = false;
                    this.queryBooks();
                })
                .catch(error => {
                    let errorMsg = "图书信息修改失败，原因：";
                    if (error.response && error.response.data) {
                        errorMsg += error.response.data;
                    }
                    console.error("修改图书错误详情:", error);
                    ElMessage.error(errorMsg);
                });
        },

        // 图书移除处理函数
        confirmRemoveBook() {
            axios.delete(`/book?bookId=${this.toRemove}`)
                .then(response => {
                    ElMessage.success("图书删除成功");
                    this.removeBookVisible = false;
                    this.queryBooks();
                })
                .catch(error => {
                    let errorMsg = "图书删除失败，原因：";
                    if (error.response && error.response.data) {
                        errorMsg += error.response.data;
                    }
                    ElMessage.error(errorMsg);
                });
        },

        // 图书库存修改处理函数
        confirmModifyStock() {
            axios.patch("/book/stock", {
                bookId: this.toModifyStockInfo.bookId,
                deltaStock: this.toModifyStockInfo.deltaStock
            })
                .then(response => {
                    ElMessage.success("库存修改成功");
                    this.modifyStockVisible = false;
                    this.queryBooks();
                })
                .catch(error => {
                    let errorMsg = "库存修改失败，原因：";
                    if (error.response && error.response.data) {
                        errorMsg += error.response.data;
                    }
                    ElMessage.error(errorMsg);
                });
        },
        
        // 高级搜索对话框
        openAdvancedSearchDialog() {
            this.advancedSearchVisible = true;
        },
        // 重置搜索条件
        resetSearchConditions() {
            this.searchConditions = {
                category: '',
                title: '',
                press: '',
                minPublishYear: '',
                maxPublishYear: '',
                author: '',
                minPrice: '',
                maxPrice: '',
                sortBy: 'book_id',
                sortOrder: 'ASC'
            };
        },
        // 高级搜索处理函数
        confirmAdvancedSearch() {
            this.books = [];
            
            let params = new URLSearchParams();
            
            if (this.searchConditions.title) {
                params.append('title', this.searchConditions.title);
            }
            if (this.searchConditions.author) {
                params.append('author', this.searchConditions.author);
            }
            if (this.searchConditions.category) {
                params.append('category', this.searchConditions.category);
            }
            if (this.searchConditions.press) {
                params.append('press', this.searchConditions.press);
            }
            if (this.searchConditions.minPublishYear) {
                params.append('minPublishYear', this.searchConditions.minPublishYear);
            }
            if (this.searchConditions.maxPublishYear) {
                params.append('maxPublishYear', this.searchConditions.maxPublishYear);
            }
            if (this.searchConditions.minPrice) {
                params.append('minPrice', this.searchConditions.minPrice);
            }
            if (this.searchConditions.maxPrice) {
                params.append('maxPrice', this.searchConditions.maxPrice);
            }
            
            params.append('sortBy', this.searchConditions.sortBy);
            params.append('sortOrder', this.searchConditions.sortOrder);
            
            axios.get(`/book/query?${params.toString()}`)
                .then(response => {
                    let books = response.data;
                    console.log("高级查询结果:", books);
                    books.forEach(book => {
                        this.books.push(book);
                    });
                    this.advancedSearchVisible = false;
                })
                .catch(error => {
                    console.error("高级查询失败:", error);
                    ElMessage.error("高级查询失败");
                });
        },
        
        // 图书列表查询函数
        queryBooks() {
            this.books = [];
            axios.get('/book/query')
                .then(response => {
                    let books = response.data;
                    console.log("获取图书列表成功:", books);
                    books.forEach(book => {
                        this.books.push(book);
                    });
                })
                .catch(error => {
                    console.error("获取图书列表失败:", error);
                    ElMessage.error("获取图书列表失败");
                });
        },
        
        // 图书批量入库对话框
        openBatchImportDialog() {
            this.batchImportVisible = true;
            this.csvPreview = [];
            this.csvData = [];
        },
        
        // 处理上传的CSV文件
        handleFileChange(file) {
            if (file && file.raw) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    try {
                        const csvContent = e.target.result;
                        const lines = csvContent.split('\n');
                        const result = [];
                        
                        // 处理每一行
                        for (let i = 0; i < lines.length; i++) {
                            const line = lines[i].trim();
                            if (!line) continue;
                            
                            const values = line.split(',');
                            if (values.length >= 7) {
                                const book = {
                                    title: values[0].trim(),
                                    author: values[1].trim(),
                                    category: values[2].trim(),
                                    press: values[3].trim(),
                                    publishYear: Number(values[4].trim()),
                                    price: Number(values[5].trim()),
                                    stock: Number(values[6].trim())
                                };
                                
                                // 验证数据有效性
                                if (book.title && book.author && book.category && book.press && 
                                    !isNaN(book.publishYear) && !isNaN(book.price) && !isNaN(book.stock)) {
                                    result.push(book);
                                }
                            }
                        }
                        
                        this.csvData = result;
                        this.csvPreview = [...result]; // 复制一份用于预览
                        
                        if (result.length === 0) {
                            ElMessage.warning('CSV文件解析结果为空，请检查文件格式');
                        } else {
                            ElMessage.success(`成功解析 ${result.length} 条图书记录`);
                        }
                    } catch (error) {
                        console.error('解析CSV文件出错:', error);
                        ElMessage.error('解析CSV文件出错，请检查文件格式');
                    }
                };
                reader.readAsText(file.raw);
            }
        },
        
        // 图书批量入库处理函数
        confirmBatchImport() {
            if (this.csvData.length === 0) {
                ElMessage.warning('没有可导入的数据');
                return;
            }
            
            // 发送批量导入请求
            axios.post("/book/batch", this.csvData)
                .then(response => {
                    ElMessage.success(`成功导入 ${this.csvData.length} 本图书`);
                    this.batchImportVisible = false;
                    this.queryBooks(); // 刷新图书列表
                })
                .catch(error => {
                    let errorMsg = "批量导入图书失败，原因：";
                    if (error.response && error.response.data) {
                        errorMsg += error.response.data;
                    }
                    console.error("批量导入错误详情:", error);
                    ElMessage.error(errorMsg);
                });
        }
    },

    // 当页面渲染完毕时，立马显示图书列表
    mounted() { 
        this.queryBooks(); 
    }
}
</script>

<style scoped>
.el-upload {
    width: 100%;
}
.el-upload-dragger {
    width: 100%;
}
</style>

